package com.digitalsignage.admin.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DeviceSessionRegistry {

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<Long, WebSocketSession> sessionsByScreen = new ConcurrentHashMap<>();

    public void register(Long screenId, WebSocketSession session) {
        WebSocketSession previous = sessionsByScreen.put(screenId, session);
        if (previous != null && previous.isOpen() && previous != session) {
            try {
                previous.close(CloseStatus.GOING_AWAY);
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    public void unregister(Long screenId, WebSocketSession session) {
        sessionsByScreen.remove(screenId, session);
    }

    public void sendConfigUpdated(Long screenId, Long layoutId) {
        WebSocketSession session = sessionsByScreen.get(screenId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "CONFIG_UPDATED",
                    "layoutId", layoutId))));
        } catch (IOException ignored) {
            // ignore
        }
    }

    public Set<Long> connectedScreenIds() {
        return Set.copyOf(sessionsByScreen.keySet());
    }

    public void sendPingAll() {
        for (WebSocketSession session : sessionsByScreen.values()) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "PING"))));
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
