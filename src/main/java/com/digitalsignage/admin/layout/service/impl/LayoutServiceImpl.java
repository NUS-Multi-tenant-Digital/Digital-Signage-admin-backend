package com.digitalsignage.admin.layout.service.impl;

import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Layout;
import com.digitalsignage.admin.entity.LayoutRegion;
import com.digitalsignage.admin.entity.LayoutRegionComponent;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.layout.dto.CreateLayoutRequest;
import com.digitalsignage.admin.layout.dto.LayoutRegionComponentRequest;
import com.digitalsignage.admin.layout.dto.LayoutRegionRequest;
import com.digitalsignage.admin.layout.dto.LayoutRegionResponse;
import com.digitalsignage.admin.layout.dto.LayoutResponse;
import com.digitalsignage.admin.layout.dto.LayoutTemplateResponse;
import com.digitalsignage.admin.layout.dto.LayoutTemplateSkeletonResponse;
import com.digitalsignage.admin.layout.dto.UpdateLayoutRequest;
import com.digitalsignage.admin.layout.repository.LayoutRegionComponentRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionRepository;
import com.digitalsignage.admin.layout.repository.LayoutRepository;
import com.digitalsignage.admin.layout.service.LayoutService;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.security.AdminPrincipal;
import com.digitalsignage.admin.websocket.ConfigPushService;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LayoutServiceImpl implements LayoutService {

    private static final int DESIGN_BASE_WIDTH = 1920;
    private static final int DESIGN_BASE_HEIGHT = 1080;
    private static final String DEFAULT_COMPONENT_TYPE = "PLAYLIST";
    private static final String EMPTY_REGION_CONFIG_JSON = "{}";

    private static final List<LayoutTemplateResponse> TEMPLATE_RESPONSES = List.of(
            LayoutTemplateResponse.builder().templateType("SINGLE_FULL").displayName("Single Full").build(),
            LayoutTemplateResponse.builder().templateType("TOP_BOTTOM").displayName("Top Bottom").build(),
            LayoutTemplateResponse.builder().templateType("LEFT_RIGHT").displayName("Left Right").build(),
            LayoutTemplateResponse.builder().templateType("MAIN_SIDEBAR").displayName("Main Sidebar").build()
    );

    private final LayoutRepository layoutRepository;
    private final LayoutRegionRepository layoutRegionRepository;
    private final LayoutRegionComponentRepository layoutRegionComponentRepository;
    private final OrganizationRepository organizationRepository;
    private final ScheduleRepository scheduleRepository;
    private final ConfigPushService configPushService;

    @Override
    @Transactional(readOnly = true)
    public List<LayoutTemplateResponse> listTemplates() {
        return TEMPLATE_RESPONSES;
    }

    @Override
    @Transactional(readOnly = true)
    public LayoutTemplateSkeletonResponse getTemplateSkeleton(
            String templateType, int resolutionWidth, int resolutionHeight) {
        if (resolutionWidth <= 0 || resolutionHeight <= 0) {
            throw new BusinessException(400, "resolution width and height must be positive");
        }
        String type = templateType == null ? "" : templateType.trim().toUpperCase();
        List<LayoutRegionRequest> baseRegions = baseRegionsForTemplate(type);
        double scaleX = resolutionWidth / (double) DESIGN_BASE_WIDTH;
        double scaleY = resolutionHeight / (double) DESIGN_BASE_HEIGHT;
        List<LayoutRegionRequest> scaled = new ArrayList<>(baseRegions.size());
        for (LayoutRegionRequest r : baseRegions) {
            scaled.add(scaleRegion(r, scaleX, scaleY));
        }
        return LayoutTemplateSkeletonResponse.builder()
                .templateType(type)
                .resolutionWidth(resolutionWidth)
                .resolutionHeight(resolutionHeight)
                .regions(List.copyOf(scaled))
                .build();
    }

    private static List<LayoutRegionRequest> baseRegionsForTemplate(String templateType) {
        return switch (templateType) {
            case "SINGLE_FULL" -> List.of(region("main", 0, 0, DESIGN_BASE_WIDTH, DESIGN_BASE_HEIGHT, 1));
            case "TOP_BOTTOM" -> List.of(
                    region("top", 0, 0, DESIGN_BASE_WIDTH, DESIGN_BASE_HEIGHT / 2, 1),
                    region("bottom", 0, DESIGN_BASE_HEIGHT / 2, DESIGN_BASE_WIDTH, DESIGN_BASE_HEIGHT / 2, 2));
            case "LEFT_RIGHT" -> List.of(
                    region("left", 0, 0, DESIGN_BASE_WIDTH / 2, DESIGN_BASE_HEIGHT, 1),
                    region("right", DESIGN_BASE_WIDTH / 2, 0, DESIGN_BASE_WIDTH / 2, DESIGN_BASE_HEIGHT, 2));
            case "MAIN_SIDEBAR" -> List.of(
                    region("main", 0, 0, (int) Math.round(DESIGN_BASE_WIDTH * 0.7), DESIGN_BASE_HEIGHT, 1),
                    region(
                            "sidebar",
                            (int) Math.round(DESIGN_BASE_WIDTH * 0.7),
                            0,
                            (int) Math.round(DESIGN_BASE_WIDTH * 0.3),
                            DESIGN_BASE_HEIGHT,
                            2));
            default -> throw new BusinessException(404, "unknown layout template type");
        };
    }

    private static LayoutRegionRequest region(String name, int x, int y, int width, int height, int zIndex) {
        LayoutRegionRequest r = new LayoutRegionRequest();
        r.setRegionName(name);
        r.setX(x);
        r.setY(y);
        r.setWidth(width);
        r.setHeight(height);
        r.setZIndex(zIndex);
        LayoutRegionComponentRequest c = new LayoutRegionComponentRequest();
        c.setComponentType(DEFAULT_COMPONENT_TYPE);
        c.setConfigJson(EMPTY_REGION_CONFIG_JSON);
        c.setSortOrder(0);
        r.setComponents(List.of(c));
        return r;
    }

    private static LayoutRegionRequest scaleRegion(LayoutRegionRequest source, double scaleX, double scaleY) {
        LayoutRegionRequest r = new LayoutRegionRequest();
        r.setRegionName(source.getRegionName());
        r.setX((int) Math.round(source.getX() * scaleX));
        r.setY((int) Math.round(source.getY() * scaleY));
        r.setWidth((int) Math.round(source.getWidth() * scaleX));
        r.setHeight((int) Math.round(source.getHeight() * scaleY));
        r.setZIndex(source.getZIndex());
        List<LayoutRegionComponentRequest> scaledComps = source.getComponents().stream()
                .map(sc -> {
                    LayoutRegionComponentRequest c = new LayoutRegionComponentRequest();
                    c.setComponentType(sc.getComponentType());
                    c.setConfigJson(sc.getConfigJson());
                    c.setSortOrder(sc.getSortOrder());
                    return c;
                })
                .toList();
        r.setComponents(scaledComps);
        return r;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LayoutResponse> listLayouts() {
        Long organizationId = currentPrincipal().getOrganizationId();
        return layoutRepository.findByOrganization_IdOrderByUpdatedAtDesc(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LayoutResponse getLayout(Long id) {
        Long organizationId = currentPrincipal().getOrganizationId();
        Layout layout = layoutRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "layout not found"));
        return toResponse(layout);
    }

    @Override
    @Transactional
    public LayoutResponse createLayout(CreateLayoutRequest request) {
        validateRegions(request.getRegions());

        Long organizationId = currentPrincipal().getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(400, "organization not found"));

        Layout layout = new Layout();
        layout.setOrganization(organization);
        layout.setName(request.getName());
        layout.setTemplateType(request.getTemplateType());
        layout.setResolutionWidth(request.getResolutionWidth());
        layout.setResolutionHeight(request.getResolutionHeight());
        layout.setStatus(request.getStatus());

        Layout savedLayout = layoutRepository.save(layout);
        saveRegions(savedLayout, request.getRegions());

        Layout foundLayout = layoutRepository.findByIdAndOrganization_Id(savedLayout.getId(), organizationId)
                .orElseThrow(() -> new BusinessException(404, "layout not found"));
        configPushService.notifyLayoutChanged(foundLayout.getId());
        return toResponse(foundLayout);
    }

    @Override
    @Transactional
    public LayoutResponse updateLayout(Long id, UpdateLayoutRequest request) {
        validateRegions(request.getRegions());

        Long organizationId = currentPrincipal().getOrganizationId();
        Layout layout = layoutRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "layout not found"));

        if (StringUtils.hasText(request.getName())) {
            layout.setName(request.getName());
        }
        if (StringUtils.hasText(request.getTemplateType())) {
            layout.setTemplateType(request.getTemplateType());
        }
        if (request.getResolutionWidth() != null) {
            layout.setResolutionWidth(request.getResolutionWidth());
        }
        if (request.getResolutionHeight() != null) {
            layout.setResolutionHeight(request.getResolutionHeight());
        }
        if (request.getStatus() != null) {
            layout.setStatus(request.getStatus());
        }

        Layout savedLayout = layoutRepository.save(layout);
        layoutRegionRepository.deleteByLayout_Id(savedLayout.getId());
        saveRegions(savedLayout, request.getRegions());

        Layout foundLayout = layoutRepository.findByIdAndOrganization_Id(savedLayout.getId(), organizationId)
                .orElseThrow(() -> new BusinessException(404, "layout not found"));
        configPushService.notifyLayoutChanged(foundLayout.getId());
        return toResponse(foundLayout);
    }

    @Override
    @Transactional
    public void deleteLayout(Long id) {
        Long organizationId = currentPrincipal().getOrganizationId();
        Layout layout = layoutRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "layout not found"));
        if (scheduleRepository.existsByLayout_Id(layout.getId())) {
            throw new BusinessException(409, "layout is referenced by schedules");
        }
        layoutRegionRepository.deleteByLayout_Id(layout.getId());
        layoutRepository.delete(layout);
    }

    private LayoutResponse toResponse(Layout layout) {
        List<LayoutRegion> regions = layoutRegionRepository.findByLayoutIdOrderBySort(layout.getId());
        Map<Long, List<LayoutRegionComponent>> byRegionId = new HashMap<>();
        if (!regions.isEmpty()) {
            List<Long> ids = regions.stream().map(LayoutRegion::getId).toList();
            List<LayoutRegionComponent> all = layoutRegionComponentRepository.findByRegion_IdIn(ids);
            for (LayoutRegionComponent c : all) {
                byRegionId.computeIfAbsent(c.getRegion().getId(), k -> new ArrayList<>()).add(c);
            }
            byRegionId.replaceAll((id, list) -> list.stream()
                    .sorted(java.util.Comparator.comparing(LayoutRegionComponent::getSortOrder)
                            .thenComparing(LayoutRegionComponent::getId))
                    .toList());
        }
        List<LayoutRegionResponse> responses = regions.stream()
                .map(r -> LayoutRegionResponse.fromEntity(r, byRegionId.getOrDefault(r.getId(), List.of())))
                .toList();
        return LayoutResponse.fromEntity(layout, responses);
    }

    private void saveRegions(Layout layout, List<LayoutRegionRequest> regionRequests) {
        for (LayoutRegionRequest request : regionRequests) {
            LayoutRegion region = new LayoutRegion();
            region.setLayout(layout);
            region.setRegionName(request.getRegionName());
            region.setX(request.getX());
            region.setY(request.getY());
            region.setWidth(request.getWidth());
            region.setHeight(request.getHeight());
            region.setZIndex(request.getZIndex());
            LayoutRegion saved = layoutRegionRepository.save(region);
            List<LayoutRegionComponentRequest> comps = request.getComponents();
            for (int i = 0; i < comps.size(); i++) {
                LayoutRegionComponentRequest cr = comps.get(i);
                LayoutRegionComponent entity = new LayoutRegionComponent();
                entity.setRegion(saved);
                entity.setComponentType(cr.getComponentType());
                entity.setConfigJson(cr.getConfigJson());
                entity.setSortOrder(cr.getSortOrder() != null ? cr.getSortOrder() : i);
                layoutRegionComponentRepository.save(entity);
            }
        }
    }

    private void validateRegions(List<LayoutRegionRequest> regions) {
        if (regions == null || regions.isEmpty()) {
            throw new BusinessException(400, "regions is required");
        }
        for (LayoutRegionRequest r : regions) {
            if (r.getComponents() == null || r.getComponents().isEmpty()) {
                throw new BusinessException(400, "each region must have at least one component");
            }
        }
    }

    private AdminPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdminPrincipal principal)) {
            throw new BusinessException(401, "unauthorized");
        }
        return principal;
    }
}
