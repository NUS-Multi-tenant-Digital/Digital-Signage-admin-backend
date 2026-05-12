package com.digitalsignage.admin.playlist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaylistItemRequest {

    @NotNull
    private Long mediaId;

    private Integer durationSeconds;

    private Integer orderIndex;
}
