package com.digitalsignage.admin.layout.service;

import com.digitalsignage.admin.layout.dto.CreateLayoutRequest;
import com.digitalsignage.admin.layout.dto.LayoutResponse;
import com.digitalsignage.admin.layout.dto.LayoutTemplateResponse;
import com.digitalsignage.admin.layout.dto.LayoutTemplateSkeletonResponse;
import com.digitalsignage.admin.layout.dto.UpdateLayoutRequest;

import java.util.List;

public interface LayoutService {

    List<LayoutTemplateResponse> listTemplates();

    LayoutTemplateSkeletonResponse getTemplateSkeleton(String templateType, int resolutionWidth, int resolutionHeight);

    List<LayoutResponse> listLayouts();

    LayoutResponse getLayout(Long id);

    LayoutResponse createLayout(CreateLayoutRequest request);

    LayoutResponse updateLayout(Long id, UpdateLayoutRequest request);

    void deleteLayout(Long id);
}
