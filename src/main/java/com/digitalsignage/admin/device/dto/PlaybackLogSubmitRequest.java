package com.digitalsignage.admin.device.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PlaybackLogSubmitRequest {

    @NotEmpty
    @Valid
    private List<Entry> entries;

    @Getter
    @Setter
    public static class Entry {

        @NotNull
        private Long mediaId;

        @NotNull
        private Long playlistId;

        private Long scheduleId;

        @NotNull
        private LocalDateTime playedAt;

        private Integer durationPlayed;
    }
}
