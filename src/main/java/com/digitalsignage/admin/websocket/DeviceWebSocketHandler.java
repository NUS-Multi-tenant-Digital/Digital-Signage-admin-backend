package com.digitalsignage.admin.websocket;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ScreenRepository screenRepository;
    private final DeviceSessionRegistry deviceSessionRegistry;
    private final DevicePresenceService devicePresenceService;

    private final Map<WebSocketSession, Long> authenticatedScreenBySession = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText("");
        switch (type) {
            case "AUTH" -> handleAuth(session, root);
            case "PONG" -> handlePong(session);
            case "STATUS_REPORT" -> handleStatusReport(session, root);
            default -> sendJson(session, Map.of("type", "AUTH_FAIL", "message", "unsupported message type"));
        }
    }

    private void handleAuth(WebSocketSession session, JsonNode root) throws IOException {
        String token = root.path("token").asText(null);
        if (!StringUtils.hasText(token)) {
            sendJson(session, Map.of("type", "AUTH_FAIL", "message", "token required"));
            return;
        }
        Optional<Screen> screenOpt = screenRepository.findByDeviceToken(token);
        if (screenOpt.isEmpty() || screenOpt.get().getActivationStatus() != ActivationStatus.ACTIVATED) {
            sendJson(session, Map.of("type", "AUTH_FAIL", "message", "invalid token"));
            return;
        }
        Screen screen = screenOpt.get();
        Long screenId = screen.getId();
        authenticatedScreenBySession.put(session, screenId);
        deviceSessionRegistry.register(screenId, session);
        devicePresenceService.markWsAuthenticated(screenId);
        sendJson(session, Map.of(
                "type", "AUTH_OK",
                "screenId", screenId));
    }

    private void handlePong(WebSocketSession session) {
        Long screenId = authenticatedScreenBySession.get(session);
        if (screenId == null) {
            return;
        }
        devicePresenceService.touchWsMessage(screenId);
    }

    private void handleStatusReport(WebSocketSession session, JsonNode root) {
        Long screenId = authenticatedScreenBySession.get(session);
        if (screenId == null) {
            return;
        }
        devicePresenceService.touchWsMessage(screenId);
        log.debug("STATUS_REPORT from screen {}: {}", screenId, root);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long screenId = authenticatedScreenBySession.remove(session);
        if (screenId != null) {
            deviceSessionRegistry.unregister(screenId, session);
            devicePresenceService.markWsDisconnected(screenId);
        }
    }

    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }
}
