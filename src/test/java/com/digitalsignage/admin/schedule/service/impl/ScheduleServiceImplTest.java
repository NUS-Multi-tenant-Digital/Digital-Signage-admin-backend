package com.digitalsignage.admin.schedule.service.impl;

import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.device.service.ActiveConfigService;
import com.digitalsignage.admin.entity.Layout;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.layout.repository.LayoutRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.dto.CreateScheduleRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckResponse;
import com.digitalsignage.admin.schedule.dto.ScheduleResponse;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.AdminPrincipal;
import com.digitalsignage.admin.screengroup.repository.ScreenGroupRepository;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import com.digitalsignage.admin.websocket.ConfigPushService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    private static final Long ORG_ID = 10L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private LayoutRepository layoutRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private ScreenGroupRepository screenGroupRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ActiveConfigService activeConfigService;

    @Mock
    private ConfigPushService configPushService;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private Organization organization;
    private Layout layout;
    private Playlist playlist;
    private Schedule existingSchedule;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(ORG_ID);

        layout = new Layout();
        layout.setId(1L);
        layout.setOrganization(organization);
        layout.setName("Main Layout");

        playlist = new Playlist();
        playlist.setId(2L);
        playlist.setOrganization(organization);
        playlist.setName("Loop");

        existingSchedule = new Schedule();
        existingSchedule.setId(100L);
        existingSchedule.setOrganization(organization);
        existingSchedule.setName("Weekday");
        existingSchedule.setTargetType(ScheduleTargetType.DEFAULT);
        existingSchedule.setLayout(layout);
        existingSchedule.setPlaylist(playlist);
        existingSchedule.setStartDatetime(LocalDateTime.of(2026, 1, 1, 8, 0));
        existingSchedule.setEndDatetime(LocalDateTime.of(2026, 1, 1, 18, 0));
        existingSchedule.setPriority(1);
        existingSchedule.setStatus(ScheduleStatus.ACTIVE);

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
    void listSchedules_success() {
        when(scheduleRepository.findByOrganization_IdOrderByUpdatedAtDesc(ORG_ID))
                .thenReturn(List.of(existingSchedule));

        List<ScheduleResponse> responses = scheduleService.listSchedules();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Weekday");
    }

    @Test
    void createSchedule_invalidTimeRange_throws400() {
        CreateScheduleRequest request = sampleCreateRequest();
        request.setStartDatetime(LocalDateTime.of(2026, 6, 1, 18, 0));
        request.setEndDatetime(LocalDateTime.of(2026, 6, 1, 8, 0));

        assertThatThrownBy(() -> scheduleService.createSchedule(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_screenTargetMissingScreenId_throws400() {
        CreateScheduleRequest request = sampleCreateRequest();
        request.setTargetType(ScheduleTargetType.SCREEN);
        request.setScreenId(null);

        assertThatThrownBy(() -> scheduleService.createSchedule(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
    }

    @Test
    void createSchedule_success() {
        CreateScheduleRequest request = sampleCreateRequest();
        request.setTargetType(ScheduleTargetType.DEFAULT);

        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(layoutRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(layout));
        when(playlistRepository.findByIdAndOrganization_Id(2L, ORG_ID)).thenReturn(Optional.of(playlist));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            s.setId(200L);
            return s;
        });

        ScheduleResponse response = scheduleService.createSchedule(request);

        assertThat(response.getName()).isEqualTo("Morning Slot");
        verify(configPushService).notifyLayoutChanged(1L);
    }

    @Test
    void checkConflict_overlappingDefaultTarget_returnsConflict() {
        ScheduleConflictCheckRequest request = new ScheduleConflictCheckRequest();
        request.setTargetType(ScheduleTargetType.DEFAULT);
        request.setStartDatetime(LocalDateTime.of(2026, 1, 1, 9, 0));
        request.setEndDatetime(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(scheduleRepository.findByOrganization_Id(ORG_ID)).thenReturn(List.of(existingSchedule));

        ScheduleConflictCheckResponse response = scheduleService.checkConflict(request);

        assertThat(response.isConflict()).isTrue();
        assertThat(response.getConflictingScheduleIds()).containsExactly(100L);
    }

    @Test
    void checkConflict_noOverlap_returnsNoConflict() {
        ScheduleConflictCheckRequest request = new ScheduleConflictCheckRequest();
        request.setTargetType(ScheduleTargetType.DEFAULT);
        request.setStartDatetime(LocalDateTime.of(2026, 1, 2, 8, 0));
        request.setEndDatetime(LocalDateTime.of(2026, 1, 2, 18, 0));

        when(scheduleRepository.findByOrganization_Id(ORG_ID)).thenReturn(List.of(existingSchedule));

        ScheduleConflictCheckResponse response = scheduleService.checkConflict(request);

        assertThat(response.isConflict()).isFalse();
        assertThat(response.getConflictingScheduleIds()).isEmpty();
    }

    @Test
    void deleteSchedule_success() {
        when(scheduleRepository.findByIdAndOrganization_Id(100L, ORG_ID))
                .thenReturn(Optional.of(existingSchedule));

        scheduleService.deleteSchedule(100L);

        verify(scheduleRepository).delete(existingSchedule);
        verify(configPushService).notifyLayoutChanged(1L);
    }

    @Test
    void getSchedule_notFound_throws404() {
        when(scheduleRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getSchedule(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void resolveForScreen_wrongOrg_throws404() {
        Organization otherOrg = new Organization();
        otherOrg.setId(99L);
        Screen screen = new Screen();
        screen.setId(1L);
        screen.setOrganization(otherOrg);

        when(screenRepository.fetchForResolve(1L)).thenReturn(Optional.of(screen));

        assertThatThrownBy(() -> scheduleService.resolveForScreen(1L, LocalDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    private CreateScheduleRequest sampleCreateRequest() {
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setName("  Morning Slot  ");
        request.setTargetType(ScheduleTargetType.DEFAULT);
        request.setLayoutId(1L);
        request.setPlaylistId(2L);
        request.setStartDatetime(LocalDateTime.of(2026, 6, 1, 8, 0));
        request.setEndDatetime(LocalDateTime.of(2026, 6, 1, 18, 0));
        request.setPriority(5);
        request.setStatus(ScheduleStatus.ACTIVE);
        return request;
    }
}
