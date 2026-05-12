package com.digitalsignage.admin.layout.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LayoutTemplateSkeletonResponse {

    private final String templateType;
    private final int resolutionWidth;
    private final int resolutionHeight;
    private final List<LayoutRegionRequest> regions;
}
