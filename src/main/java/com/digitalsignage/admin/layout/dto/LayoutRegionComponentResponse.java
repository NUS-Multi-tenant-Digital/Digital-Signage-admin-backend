package com.digitalsignage.admin.layout.dto;

import com.digitalsignage.admin.entity.LayoutRegionComponent;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LayoutRegionComponentResponse {

    private Long id;
    private String componentType;
    private String configJson;
    private Integer sortOrder;

    public static LayoutRegionComponentResponse fromEntity(LayoutRegionComponent c) {
        return LayoutRegionComponentResponse.builder()
                .id(c.getId())
                .componentType(c.getComponentType())
                .configJson(c.getConfigJson())
                .sortOrder(c.getSortOrder())
                .build();
    }
}
