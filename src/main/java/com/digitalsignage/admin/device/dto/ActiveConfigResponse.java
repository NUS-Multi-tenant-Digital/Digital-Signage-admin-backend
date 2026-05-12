package com.digitalsignage.admin.device.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ActiveConfigResponse {

    private Long scheduleId;
    private LocalDateTime resolvedAt;
    private LocalDateTime scheduleStart;
    private LocalDateTime scheduleEnd;
    private Integer priority;
    private LayoutPayload layout;
    private PlaylistPayload playlist;

    @Getter
    @Builder
    public static class LayoutPayload {
        private Long id;
        private String name;
        private String templateType;
        private Integer resolutionWidth;
        private Integer resolutionHeight;
        private List<RegionPayload> regions;
    }

    @Getter
    @Builder
    public static class RegionPayload {
        private Long id;
        private String regionName;
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;
        private Integer zIndex;
        private List<RegionComponentPayload> components;
    }

    @Getter
    @Builder
    public static class RegionComponentPayload {
        private Long id;
        private String componentType;
        private String configJson;
        private Integer sortOrder;
    }

    @Getter
    @Builder
    public static class PlaylistPayload {
        private Long id;
        private String name;
        private List<ItemPayload> items;
    }

    @Getter
    @Builder
    public static class ItemPayload {
        private Long mediaId;
        private String mediaType;
        private String name;
        private String fileUrl;
        private String thumbnailUrl;
        private Integer durationSeconds;
        private Integer orderIndex;
    }
}
