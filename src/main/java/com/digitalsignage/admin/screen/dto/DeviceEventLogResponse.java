package com.digitalsignage.admin.screen.dto;

import com.digitalsignage.admin.entity.DeviceEventLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeviceEventLogResponse {

    private Long id;
    private String eventId;
    private String eventType;
    private String eventLevel;
    private String message;
    private String manifestId;
    private Long manifestVersion;
    private Long mediaId;
    private String playlistItemId;
    private String errorCode;
    private String errorMessage;
    private String extraJson;
    private Long eventTimestamp;
    private LocalDateTime createdAt;

    public static DeviceEventLogResponse fromEntity(DeviceEventLog log) {
        return DeviceEventLogResponse.builder()
                .id(log.getId())
                .eventId(log.getEventId())
                .eventType(log.getEventType())
                .eventLevel(log.getEventLevel().name())
                .message(log.getMessage())
                .manifestId(log.getManifestId())
                .manifestVersion(log.getManifestVersion())
                .mediaId(log.getMediaId())
                .playlistItemId(log.getPlaylistItemId())
                .errorCode(log.getErrorCode())
                .errorMessage(log.getErrorMessage())
                .extraJson(log.getExtraJson())
                .eventTimestamp(log.getEventTimestamp())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
