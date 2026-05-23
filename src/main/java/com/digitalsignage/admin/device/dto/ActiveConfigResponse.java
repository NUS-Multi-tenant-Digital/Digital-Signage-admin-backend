package com.digitalsignage.admin.device.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@ToString
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
    @ToString
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
    @ToString
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
    @ToString
    public static class RegionComponentPayload {
        private Long id;
        private String componentType;
        private String configJson;
        private Integer sortOrder;
    }

    @Getter
    @Builder
    @ToString
    public static class PlaylistPayload {
        private Long id;
        private String name;
        private List<ItemPayload> items;
    }

    @Getter
    @Builder
    @ToString
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
