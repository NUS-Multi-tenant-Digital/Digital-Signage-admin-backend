package com.digitalsignage.admin.screen.dto;

import com.digitalsignage.admin.entity.DeviceEventLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeviceEventLogResponse {

    private Long id;
    private String eventType;
    private String eventLevel;
    private String message;
    private LocalDateTime createdAt;

    public static DeviceEventLogResponse fromEntity(DeviceEventLog log) {
        return DeviceEventLogResponse.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .eventLevel(log.getEventLevel().name())
                .message(log.getMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
