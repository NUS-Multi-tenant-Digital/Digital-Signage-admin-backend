package com.digitalsignage.admin.layout.dto;

import com.digitalsignage.admin.entity.LayoutRegion;
import com.digitalsignage.admin.entity.LayoutRegionComponent;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class LayoutRegionResponse {

    private Long id;
    private String regionName;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private Integer zIndex;
    private List<LayoutRegionComponentResponse> components;

    public static LayoutRegionResponse fromEntity(LayoutRegion region, List<LayoutRegionComponent> components) {
        List<LayoutRegionComponentResponse> compResponses = components.stream()
                .sorted(Comparator.comparing(LayoutRegionComponent::getSortOrder).thenComparing(LayoutRegionComponent::getId))
                .map(LayoutRegionComponentResponse::fromEntity)
                .toList();
        return LayoutRegionResponse.builder()
                .id(region.getId())
                .regionName(region.getRegionName())
                .x(region.getX())
                .y(region.getY())
                .width(region.getWidth())
                .height(region.getHeight())
                .zIndex(region.getZIndex())
                .components(compResponses)
                .build();
    }
}
