package com.digitalsignage.admin.websocket;

import com.digitalsignage.admin.common.enums.ScreenStatus;
import com.digitalsignage.admin.common.enums.WsStatus;
import com.digitalsignage.admin.device.config.DevicePresenceProperties;
import com.digitalsignage.admin.device.dto.DeviceHeartbeatRequest;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DevicePresenceService {

    private final ScreenRepository screenRepository;
    private final DevicePresenceProperties presenceProperties;

    @Transactional
    public void markWsAuthenticated(Long screenId) {
        Screen screen = screenRepository.findById(screenId).orElseThrow();
        screen.setWsStatus(WsStatus.CONNECTED);
        LocalDateTime now = LocalDateTime.now();
        screen.setLastWsConnectedAt(now);
        screen.setLastWsMessageAt(now);
        screen.setProbeFailCount(0);
        promoteOnlineIfApplicable(screen);
        screenRepository.save(screen);
    }

    @Transactional
    public void touchWsMessage(Long screenId) {
        Screen screen = screenRepository.findById(screenId).orElseThrow();
        screen.setLastWsMessageAt(LocalDateTime.now());
        screen.setWsStatus(WsStatus.CONNECTED);
        screen.setProbeFailCount(0);
        promoteOnlineIfApplicable(screen);
        screenRepository.save(screen);
    }

    @Transactional
    public void markWsDisconnected(Long screenId) {
        Screen screen = screenRepository.findById(screenId).orElseThrow();
        screen.setWsStatus(WsStatus.DISCONNECTED);
        screen.setStatus(ScreenStatus.OFFLINE);
        screenRepository.save(screen);
    }

    /**
     * WS 长时间无消息（含未按时 PONG）：递增探测失败并按阈值降级。
     */
    @Transactional
    public void applyWsSilenceFailure(Long screenId) {
        Screen screen = screenRepository.findById(screenId).orElseThrow();
        int fails = screen.getProbeFailCount() == null ? 0 : screen.getProbeFailCount();
        fails++;
        screen.setProbeFailCount(fails);
        if (fails >= presenceProperties.getOfflineProbeThreshold()) {
            screen.setStatus(ScreenStatus.OFFLINE);
            screen.setWsStatus(WsStatus.DISCONNECTED);
        } else if (fails >= presenceProperties.getSuspectProbeThreshold()) {
            if (screen.getStatus() == ScreenStatus.ONLINE) {
                screen.setStatus(ScreenStatus.SUSPECT);
            }
        }
        screenRepository.save(screen);
    }

    /**
     * 连接仍在但 HTTP 心跳过久：标记疑似（运行侧失联）。
     */
    @Transactional
    public void applyStaleHttpHeartbeat(Long screenId) {
        Screen screen = screenRepository.findById(screenId).orElseThrow();
        if (screen.getWsStatus() == WsStatus.CONNECTED && screen.getStatus() == ScreenStatus.ONLINE) {
            screen.setStatus(ScreenStatus.SUSPECT);
            screenRepository.save(screen);
        }
    }

    @Transactional
    public void applyHttpHeartbeat(Screen screen, DeviceHeartbeatRequest request) {
        LocalDateTime now = LocalDateTime.now();
        screen.setLastHeartbeatAt(now);
        if (request.getAppVersion() != null) {
            screen.setAppVersion(request.getAppVersion());
        }
        if (request.getResolutionWidth() != null) {
            screen.setResolutionWidth(request.getResolutionWidth());
        }
        if (request.getResolutionHeight() != null) {
            screen.setResolutionHeight(request.getResolutionHeight());
        }

        if (Boolean.FALSE.equals(request.getRuntimeHealthy())) {
            screen.setStatus(ScreenStatus.ERROR);
        } else {
            if (screen.getStatus() == ScreenStatus.ERROR) {
                screen.setStatus(ScreenStatus.ONLINE);
            }
        }
        screenRepository.save(screen);
    }

    private static void promoteOnlineIfApplicable(Screen screen) {
        if (screen.getStatus() == ScreenStatus.OFFLINE || screen.getStatus() == ScreenStatus.SUSPECT) {
            screen.setStatus(ScreenStatus.ONLINE);
        }
    }
}
