package com.digitalsignage.admin.emergency.service.impl;

import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.emergency.EmergencyOverrideConstants;
import com.digitalsignage.admin.emergency.dto.ActivateEmergencyOverrideRequest;
import com.digitalsignage.admin.emergency.dto.EmergencyOverrideResponse;
import com.digitalsignage.admin.entity.Layout;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.PlaylistItem;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.layout.repository.LayoutRegionComponentRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionRepository;
import com.digitalsignage.admin.layout.repository.LayoutRepository;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
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
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyOverrideServiceImplTest {

    private static final Long ORG_ID = 10L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private LayoutRepository layoutRepository;

    @Mock
    private LayoutRegionRepository layoutRegionRepository;

    @Mock
    private LayoutRegionComponentRepository layoutRegionComponentRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistItemRepository playlistItemRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private ScreenGroupRepository screenGroupRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ConfigPushService configPushService;

    @InjectMocks
    private EmergencyOverrideServiceImpl emergencyOverrideService;

    private Organization organization;
    private Media media;
    private Screen screen;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(ORG_ID);

        media = new Media();
        media.setId(5L);
        media.setDurationSeconds(30);

        screen = new Screen();
        screen.setId(99L);
        screen.setOrganization(organization);

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
    void activate_createsHighPriorityEmergencyScheduleAndPushesConfig() {
        ActivateEmergencyOverrideRequest request = new ActivateEmergencyOverrideRequest();
        request.setName("Fire drill");
        request.setMediaId(5L);
        request.setTargetType(ScheduleTargetType.SCREEN);
        request.setScreenId(99L);
        request.setDurationMinutes(15);

        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(mediaRepository.findByIdAndOrganization_Id(5L, ORG_ID)).thenReturn(Optional.of(media));
        when(scheduleRepository.findActiveEmergencySchedules(eq(ORG_ID), eq(EmergencyOverrideConstants.SCHEDULE_NAME_PREFIX), any()))
                .thenReturn(List.of());
        when(screenRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.of(screen));
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(inv -> {
            Playlist p = inv.getArgument(0);
            p.setId(20L);
            return p;
        });
        when(layoutRepository.save(any(Layout.class))).thenAnswer(inv -> {
            Layout l = inv.getArgument(0);
            l.setId(30L);
            return l;
        });
        when(layoutRegionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(layoutRegionComponentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(playlistItemRepository.save(any(PlaylistItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            s.setId(40L);
            s.setLayout(new Layout());
            s.getLayout().setId(30L);
            s.setPlaylist(new Playlist());
            s.getPlaylist().setId(20L);
            s.setScreen(screen);
            return s;
        });

        EmergencyOverrideResponse response = emergencyOverrideService.activate(request);

        assertThat(response.getScheduleId()).isEqualTo(40L);
        assertThat(response.getName()).isEqualTo("EMERGENCY:Fire drill");
        assertThat(response.getDisplayName()).isEqualTo("Fire drill");
        assertThat(response.getPriority()).isEqualTo(EmergencyOverrideConstants.PRIORITY);
        assertThat(response.getMediaId()).isEqualTo(5L);

        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue().getPriority()).isEqualTo(EmergencyOverrideConstants.PRIORITY);
        assertThat(scheduleCaptor.getValue().getStatus()).isEqualTo(ScheduleStatus.ACTIVE);
        verify(configPushService).notifyLayoutChanged(30L);
    }

    @Test
    void cancel_rejectsNonEmergencySchedule() {
        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setName("Normal schedule");
        schedule.setStatus(ScheduleStatus.ACTIVE);
        schedule.setLayout(new Layout());
        schedule.getLayout().setId(7L);

        when(scheduleRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> emergencyOverrideService.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not an emergency override");
    }
}
