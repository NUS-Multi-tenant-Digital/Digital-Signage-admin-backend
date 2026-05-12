package com.digitalsignage.admin.layout.service.impl;

import com.digitalsignage.admin.common.enums.LayoutStatus;
import com.digitalsignage.admin.common.enums.OrganizationStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Layout;
import com.digitalsignage.admin.entity.LayoutRegion;
import com.digitalsignage.admin.entity.LayoutRegionComponent;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.layout.dto.CreateLayoutRequest;
import com.digitalsignage.admin.layout.dto.LayoutRegionComponentRequest;
import com.digitalsignage.admin.layout.dto.LayoutRegionRequest;
import com.digitalsignage.admin.layout.dto.LayoutTemplateSkeletonResponse;
import com.digitalsignage.admin.layout.dto.LayoutResponse;
import com.digitalsignage.admin.layout.dto.UpdateLayoutRequest;
import com.digitalsignage.admin.layout.repository.LayoutRegionComponentRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionRepository;
import com.digitalsignage.admin.layout.repository.LayoutRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.security.AdminPrincipal;
import com.digitalsignage.admin.websocket.ConfigPushService;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LayoutServiceImplTest {

    @Mock
    private LayoutRepository layoutRepository;

    @Mock
    private LayoutRegionRepository layoutRegionRepository;

    @Mock
    private LayoutRegionComponentRepository layoutRegionComponentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ConfigPushService configPushService;

    @InjectMocks
    private LayoutServiceImpl layoutService;

    private Organization organization;
    private Layout layout;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(10L);
        organization.setName("Org A");
        organization.setCode("ORG_A");
        organization.setStatus(OrganizationStatus.ACTIVE);

        layout = new Layout();
        layout.setId(1L);
        layout.setOrganization(organization);
        layout.setName("Layout A");
        layout.setTemplateType("SINGLE_FULL");
        layout.setResolutionWidth(1920);
        layout.setResolutionHeight(1080);
        layout.setStatus(LayoutStatus.DRAFT);

        AdminPrincipal principal = AdminPrincipal.builder()
                .userId(99L)
                .organizationId(10L)
                .username("admin")
                .role(UserRole.ADMIN)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listLayouts_success() {
        LayoutRegion region = sampleRegion(layout);
        when(layoutRepository.findByOrganization_IdOrderByUpdatedAtDesc(10L)).thenReturn(List.of(layout));
        when(layoutRegionRepository.findByLayoutIdOrderBySort(1L)).thenReturn(List.of(region));
        when(layoutRegionComponentRepository.findByRegion_IdIn(anyList())).thenReturn(List.of(sampleComponent(region)));

        List<LayoutResponse> responses = layoutService.listLayouts();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Layout A");
        assertThat(responses.get(0).getRegions()).hasSize(1);
    }

    @Test
    void getLayout_notFound_throws404() {
        when(layoutRepository.findByIdAndOrganization_Id(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> layoutService.getLayout(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void createLayout_success() {
        CreateLayoutRequest request = new CreateLayoutRequest();
        request.setName("New Layout");
        request.setTemplateType("TOP_BOTTOM");
        request.setResolutionWidth(1920);
        request.setResolutionHeight(1080);
        request.setStatus(LayoutStatus.PUBLISHED);
        request.setRegions(List.of(sampleRegionRequest()));

        Layout saved = new Layout();
        saved.setId(5L);
        saved.setOrganization(organization);
        saved.setName(request.getName());
        saved.setTemplateType(request.getTemplateType());
        saved.setResolutionWidth(request.getResolutionWidth());
        saved.setResolutionHeight(request.getResolutionHeight());
        saved.setStatus(request.getStatus());

        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(layoutRepository.save(any(Layout.class))).thenReturn(saved);
        when(layoutRepository.findByIdAndOrganization_Id(5L, 10L)).thenReturn(Optional.of(saved));
        LayoutRegion persistedRegion = sampleRegion(saved);
        when(layoutRegionRepository.save(any(LayoutRegion.class))).thenAnswer(inv -> {
            LayoutRegion r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(layoutRegionRepository.findByLayoutIdOrderBySort(5L)).thenReturn(List.of(persistedRegion));
        when(layoutRegionComponentRepository.findByRegion_IdIn(anyList())).thenReturn(List.of(sampleComponent(persistedRegion)));

        LayoutResponse response = layoutService.createLayout(request);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getTemplateType()).isEqualTo("TOP_BOTTOM");
        verify(layoutRegionRepository).save(any(LayoutRegion.class));
        verify(layoutRegionComponentRepository).save(any(LayoutRegionComponent.class));
        verify(configPushService).notifyLayoutChanged(5L);
    }

    @Test
    void updateLayout_success() {
        UpdateLayoutRequest request = new UpdateLayoutRequest();
        request.setName("Layout Updated");
        request.setStatus(LayoutStatus.PUBLISHED);
        request.setRegions(List.of(sampleRegionRequest()));

        Layout updated = new Layout();
        updated.setId(1L);
        updated.setOrganization(organization);
        updated.setName("Layout Updated");
        updated.setTemplateType("SINGLE_FULL");
        updated.setResolutionWidth(1920);
        updated.setResolutionHeight(1080);
        updated.setStatus(LayoutStatus.PUBLISHED);

        when(layoutRepository.findByIdAndOrganization_Id(1L, 10L)).thenReturn(Optional.of(layout), Optional.of(updated));
        when(layoutRepository.save(any(Layout.class))).thenReturn(updated);
        doNothing().when(layoutRegionRepository).deleteByLayout_Id(1L);
        LayoutRegion persistedRegion = sampleRegion(updated);
        when(layoutRegionRepository.save(any(LayoutRegion.class))).thenAnswer(inv -> {
            LayoutRegion r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(layoutRegionRepository.findByLayoutIdOrderBySort(1L)).thenReturn(List.of(persistedRegion));
        when(layoutRegionComponentRepository.findByRegion_IdIn(anyList())).thenReturn(List.of(sampleComponent(persistedRegion)));

        LayoutResponse response = layoutService.updateLayout(1L, request);

        assertThat(response.getStatus()).isEqualTo(LayoutStatus.PUBLISHED);
        verify(layoutRegionRepository).deleteByLayout_Id(1L);
        verify(layoutRegionRepository).save(any(LayoutRegion.class));
        verify(layoutRegionComponentRepository).save(any(LayoutRegionComponent.class));
        verify(configPushService).notifyLayoutChanged(1L);
    }

    @Test
    void deleteLayout_success() {
        when(layoutRepository.findByIdAndOrganization_Id(1L, 10L)).thenReturn(Optional.of(layout));
        when(scheduleRepository.existsByLayout_Id(1L)).thenReturn(false);

        layoutService.deleteLayout(1L);

        verify(layoutRegionRepository).deleteByLayout_Id(1L);
        verify(layoutRepository).delete(layout);
    }

    @Test
    void createLayout_missingRegions_throws400() {
        CreateLayoutRequest request = new CreateLayoutRequest();
        request.setName("X");
        request.setTemplateType("SINGLE_FULL");
        request.setResolutionWidth(1);
        request.setResolutionHeight(1);
        request.setStatus(LayoutStatus.DRAFT);
        request.setRegions(List.of());

        assertThatThrownBy(() -> layoutService.createLayout(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
        verify(layoutRepository, never()).save(any(Layout.class));
    }

    @Test
    void noPrincipal_throws401() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> layoutService.listLayouts())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 401);
    }

    @Test
    void getTemplateSkeleton_singleFull_atDesignResolution() {
        LayoutTemplateSkeletonResponse skeleton =
                layoutService.getTemplateSkeleton("SINGLE_FULL", 1920, 1080);

        assertThat(skeleton.getTemplateType()).isEqualTo("SINGLE_FULL");
        assertThat(skeleton.getResolutionWidth()).isEqualTo(1920);
        assertThat(skeleton.getResolutionHeight()).isEqualTo(1080);
        assertThat(skeleton.getRegions()).hasSize(1);
        assertThat(skeleton.getRegions().get(0).getRegionName()).isEqualTo("main");
        assertThat(skeleton.getRegions().get(0).getWidth()).isEqualTo(1920);
        assertThat(skeleton.getRegions().get(0).getHeight()).isEqualTo(1080);
        assertThat(skeleton.getRegions().get(0).getComponents().get(0).getConfigJson()).isEqualTo("{}");
    }

    @Test
    void getTemplateSkeleton_normalizesTemplateTypeCase() {
        LayoutTemplateSkeletonResponse skeleton =
                layoutService.getTemplateSkeleton("single_full", 1920, 1080);

        assertThat(skeleton.getTemplateType()).isEqualTo("SINGLE_FULL");
    }

    @Test
    void getTemplateSkeleton_unknownTemplate_throws404() {
        assertThatThrownBy(() -> layoutService.getTemplateSkeleton("UNKNOWN", 1920, 1080))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void getTemplateSkeleton_invalidResolution_throws400() {
        assertThatThrownBy(() -> layoutService.getTemplateSkeleton("SINGLE_FULL", 0, 1080))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
    }

    @Test
    void getTemplateSkeleton_scalesRegionsToTargetResolution() {
        LayoutTemplateSkeletonResponse skeleton =
                layoutService.getTemplateSkeleton("LEFT_RIGHT", 3840, 2160);

        assertThat(skeleton.getRegions().get(0).getWidth()).isEqualTo(1920);
        assertThat(skeleton.getRegions().get(1).getX()).isEqualTo(1920);
    }

    private static LayoutRegionRequest sampleRegionRequest() {
        LayoutRegionComponentRequest comp = new LayoutRegionComponentRequest();
        comp.setComponentType("PLAYLIST");
        comp.setConfigJson("{\"playlistId\":1}");
        comp.setSortOrder(0);
        LayoutRegionRequest req = new LayoutRegionRequest();
        req.setRegionName("main");
        req.setX(0);
        req.setY(0);
        req.setWidth(1920);
        req.setHeight(1080);
        req.setZIndex(1);
        req.setComponents(List.of(comp));
        return req;
    }

    private static LayoutRegion sampleRegion(Layout layout) {
        LayoutRegion region = new LayoutRegion();
        region.setId(100L);
        region.setLayout(layout);
        region.setRegionName("main");
        region.setX(0);
        region.setY(0);
        region.setWidth(1920);
        region.setHeight(1080);
        region.setZIndex(1);
        return region;
    }

    private static LayoutRegionComponent sampleComponent(LayoutRegion region) {
        LayoutRegionComponent c = new LayoutRegionComponent();
        c.setId(10L);
        c.setRegion(region);
        c.setComponentType("PLAYLIST");
        c.setConfigJson("{\"playlistId\":1}");
        c.setSortOrder(0);
        return c;
    }
}
