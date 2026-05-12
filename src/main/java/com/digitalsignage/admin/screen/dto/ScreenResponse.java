package com.digitalsignage.admin.screen.dto;

import com.digitalsignage.admin.entity.Screen;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScreenResponse {

    private Long id;
    private String deviceCode;
    private String name;
    private Long screenGroupId;
    private String screenGroupName;
    private String activationStatus;
    private String status;
    private String wsStatus;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime lastWsConnectedAt;
    private LocalDateTime lastWsMessageAt;
    private Integer resolutionWidth;
    private Integer resolutionHeight;
    private String appVersion;

    public static ScreenResponse fromEntity(Screen screen) {
        var builder = ScreenResponse.builder()
                .id(screen.getId())
                .deviceCode(screen.getDeviceCode())
                .name(screen.getName())
                .activationStatus(screen.getActivationStatus().name())
                .status(screen.getStatus().name())
                .wsStatus(screen.getWsStatus().name())
                .lastHeartbeatAt(screen.getLastHeartbeatAt())
                .lastWsConnectedAt(screen.getLastWsConnectedAt())
                .lastWsMessageAt(screen.getLastWsMessageAt())
                .resolutionWidth(screen.getResolutionWidth())
                .resolutionHeight(screen.getResolutionHeight())
                .appVersion(screen.getAppVersion());
        if (screen.getScreenGroup() != null) {
            builder.screenGroupId(screen.getScreenGroup().getId())
                    .screenGroupName(screen.getScreenGroup().getName());
        }
        return builder.build();
    }
}
