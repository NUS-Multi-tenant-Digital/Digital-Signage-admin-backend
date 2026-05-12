package com.digitalsignage.admin.websocket;

import com.digitalsignage.admin.common.enums.WsStatus;
import com.digitalsignage.admin.device.config.DevicePresenceProperties;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * WebSocket 保活探测与离线判定（用例 17、Modules「在线状态」）。
 */
@Component
@RequiredArgsConstructor
public class DevicePresenceMonitor {

    private final DeviceSessionRegistry deviceSessionRegistry;
    private final ScreenRepository screenRepository;
    private final DevicePresenceService devicePresenceService;
    private final DevicePresenceProperties presenceProperties;

    @Scheduled(fixedRateString = "${app.device.presence.monitor-interval-ms:30000}")
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime wsSilenceBefore = now.minusSeconds(presenceProperties.getWsSilenceThresholdSeconds());
        LocalDateTime heartbeatStaleBefore = now.minusSeconds(presenceProperties.getHeartbeatStaleSeconds());

        for (Long screenId : deviceSessionRegistry.connectedScreenIds()) {
            Screen screen = screenRepository.findById(screenId).orElse(null);
            if (screen == null || screen.getWsStatus() != WsStatus.CONNECTED) {
                continue;
            }
            LocalDateTime lastWs = screen.getLastWsMessageAt();
            if (lastWs == null || lastWs.isBefore(wsSilenceBefore)) {
                devicePresenceService.applyWsSilenceFailure(screenId);
            }
            LocalDateTime lastHb = screen.getLastHeartbeatAt();
            if (lastHb != null && lastHb.isBefore(heartbeatStaleBefore)) {
                devicePresenceService.applyStaleHttpHeartbeat(screenId);
            }
        }

        deviceSessionRegistry.sendPingAll();
    }
}
