package com.digitalsignage.admin.screen.service.impl;

import com.digitalsignage.admin.common.enums.DeviceEventLevel;
import com.digitalsignage.admin.common.enums.MediaType;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.device.repository.DeviceEventLogRepository;
import com.digitalsignage.admin.device.repository.PlaybackLogRepository;
import com.digitalsignage.admin.entity.DeviceEventLog;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.PlaybackLog;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.screen.dto.DeviceEventLogResponse;
import com.digitalsignage.admin.screen.dto.PlaybackLogAdminResponse;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.AdminPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreenLogServiceImplTest {

    private static final Long ORG_ID = 10L;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private DeviceEventLogRepository deviceEventLogRepository;

    @Mock
    private PlaybackLogRepository playbackLogRepository;

    @InjectMocks
    private ScreenLogServiceImpl screenLogService;

    @BeforeEach
    void setUp() {
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
    void listEvents_success() {
        Organization org = new Organization();
        org.setId(ORG_ID);
        Screen screen = new Screen();
        screen.setId(1L);
        screen.setOrganization(org);

        DeviceEventLog log = new DeviceEventLog();
        log.setId(100L);
        log.setScreen(screen);
        log.setEventType("ONLINE");
        log.setEventLevel(DeviceEventLevel.INFO);

        when(screenRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(screen));
        when(deviceEventLogRepository.findByScreen_IdOrderByEventTimestampDescCreatedAtDesc(
                eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<DeviceEventLogResponse> page = screenLogService.listEvents(1L, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getEventType()).isEqualTo("ONLINE");
    }

    @Test
    void listEvents_screenNotInTenant_throws404() {
        when(screenRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> screenLogService.listEvents(99L, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void listPlaybackLogs_success() {
        Organization org = new Organization();
        org.setId(ORG_ID);
        Screen screen = new Screen();
        screen.setId(1L);
        screen.setOrganization(org);

        Media media = new Media();
        media.setId(50L);
        media.setName("clip.mp4");
        media.setMediaType(MediaType.VIDEO);

        Playlist playlist = new Playlist();
        playlist.setId(7L);

        PlaybackLog log = new PlaybackLog();
        log.setId(200L);
        log.setScreen(screen);
        log.setMedia(media);
        log.setPlaylist(playlist);

        when(screenRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(screen));
        when(playbackLogRepository.findByScreen_IdOrderByPlayedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<PlaybackLogAdminResponse> page = screenLogService.listPlaybackLogs(1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(200L);
    }
}
