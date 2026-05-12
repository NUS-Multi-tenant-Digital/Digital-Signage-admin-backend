package com.digitalsignage.admin.screen.dto;

import com.digitalsignage.admin.entity.PlaybackLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PlaybackLogAdminResponse {

    private Long id;
    private LocalDateTime playedAt;
    private Long mediaId;
    private String mediaName;
    private Long playlistId;
    private Long scheduleId;
    private Integer durationPlayed;

    public static PlaybackLogAdminResponse fromEntity(PlaybackLog log) {
        return PlaybackLogAdminResponse.builder()
                .id(log.getId())
                .playedAt(log.getPlayedAt())
                .mediaId(log.getMedia().getId())
                .mediaName(log.getMedia().getName())
                .playlistId(log.getPlaylist().getId())
                .scheduleId(log.getSchedule() != null ? log.getSchedule().getId() : null)
                .durationPlayed(log.getDurationPlayed())
                .build();
    }
}
