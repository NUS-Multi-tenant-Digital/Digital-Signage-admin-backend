package com.digitalsignage.admin.playlist.dto;

import com.digitalsignage.admin.entity.PlaylistItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaylistItemResponse {

    private Long id;
    private Long mediaId;
    private Integer orderIndex;
    private Integer durationSeconds;

    public static PlaylistItemResponse fromEntity(PlaylistItem item) {
        return PlaylistItemResponse.builder()
                .id(item.getId())
                .mediaId(item.getMedia().getId())
                .orderIndex(item.getOrderIndex())
                .durationSeconds(item.getDurationSeconds())
                .build();
    }
}
