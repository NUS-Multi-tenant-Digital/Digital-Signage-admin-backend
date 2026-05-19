package com.digitalsignage.admin.screengroup.service.impl;

import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.ScreenGroup;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.screengroup.dto.CreateScreenGroupRequest;
import com.digitalsignage.admin.screengroup.dto.ScreenGroupResponse;
import com.digitalsignage.admin.screengroup.dto.UpdateScreenGroupRequest;
import com.digitalsignage.admin.screengroup.repository.ScreenGroupRepository;
import com.digitalsignage.admin.security.AdminPrincipal;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreenGroupServiceImplTest {

    private static final Long ORG_ID = 10L;

    @Mock
    private ScreenGroupRepository screenGroupRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private ScreenGroupServiceImpl screenGroupService;

    private Organization organization;
    private ScreenGroup group;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(ORG_ID);

        group = new ScreenGroup();
        group.setId(1L);
        group.setOrganization(organization);
        group.setName("Floor 1");
        group.setLocation("Building A");

        AdminPrincipal principal = AdminPrincipal.builder()
                .userId(1L)
                .organizationId(ORG_ID)
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
    void listGroups_success() {
        when(screenGroupRepository.findByOrganization_IdOrderByUpdatedAtDesc(ORG_ID))
                .thenReturn(List.of(group));

        List<ScreenGroupResponse> responses = screenGroupService.listGroups();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Floor 1");
    }

    @Test
    void createGroup_success() {
        CreateScreenGroupRequest request = new CreateScreenGroupRequest();
        request.setName("  New Group  ");
        request.setLocation("Lobby");

        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));

        ScreenGroup saved = new ScreenGroup();
        saved.setId(2L);
        saved.setOrganization(organization);
        saved.setName("New Group");
        saved.setLocation("Lobby");

        when(screenGroupRepository.save(any(ScreenGroup.class))).thenReturn(saved);

        ScreenGroupResponse response = screenGroupService.createGroup(request);

        assertThat(response.getName()).isEqualTo("New Group");
        assertThat(response.getLocation()).isEqualTo("Lobby");
    }

    @Test
    void updateGroup_success() {
        UpdateScreenGroupRequest request = new UpdateScreenGroupRequest();
        request.setName("Renamed");
        request.setLocation("Floor 2");

        when(screenGroupRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(group));
        when(screenGroupRepository.save(any(ScreenGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        ScreenGroupResponse response = screenGroupService.updateGroup(1L, request);

        assertThat(response.getName()).isEqualTo("Renamed");
        assertThat(response.getLocation()).isEqualTo("Floor 2");
    }

    @Test
    void deleteGroup_hasScreens_throws409() {
        when(screenGroupRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(group));
        when(screenRepository.existsByScreenGroup_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> screenGroupService.deleteGroup(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 409);
        verify(screenGroupRepository, never()).delete(any());
    }

    @Test
    void deleteGroup_referencedBySchedule_throws409() {
        when(screenGroupRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(group));
        when(screenRepository.existsByScreenGroup_Id(1L)).thenReturn(false);
        when(scheduleRepository.existsByScreenGroup_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> screenGroupService.deleteGroup(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 409);
    }

    @Test
    void deleteGroup_success() {
        when(screenGroupRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(group));
        when(screenRepository.existsByScreenGroup_Id(1L)).thenReturn(false);
        when(scheduleRepository.existsByScreenGroup_Id(1L)).thenReturn(false);

        screenGroupService.deleteGroup(1L);

        verify(screenGroupRepository).delete(group);
    }

    @Test
    void getGroup_notFound_throws404() {
        when(screenGroupRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> screenGroupService.getGroup(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }
}
