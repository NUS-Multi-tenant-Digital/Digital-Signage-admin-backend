package com.digitalsignage.admin.device.service.impl;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.device.dto.DeviceActivateRequest;
import com.digitalsignage.admin.device.dto.DeviceActivateResponse;
import com.digitalsignage.admin.device.dto.PlaybackLogSubmitRequest;
import com.digitalsignage.admin.device.repository.PlaybackLogRepository;
import com.digitalsignage.admin.device.service.ActiveConfigService;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.DevicePrincipal;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private ActiveConfigService activeConfigService;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private PlaybackLogRepository playbackLogRepository;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @BeforeEach
    void setUpDevicePrincipal() {
        DevicePrincipal principal = DevicePrincipal.builder()
                .screenId(9L)
                .organizationId(10L)
                .deviceCode("dev1")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activate_success_setsActivatedAndReturnsToken() {
        Screen screen = new Screen();
        screen.setId(9L);
        screen.setActivationStatus(ActivationStatus.PENDING);
        screen.setActivationCode("ABC123");

        when(screenRepository.findByDeviceCode("dev1")).thenReturn(Optional.of(screen));
        when(screenRepository.save(any(Screen.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceActivateRequest req = new DeviceActivateRequest();
        req.setDeviceCode("dev1");
        req.setActivationCode("ABC123");

        DeviceActivateResponse res = deviceService.activate(req);

        assertThat(res.getScreenId()).isEqualTo(9L);
        assertThat(res.getDeviceToken()).isNotBlank();
        assertThat(screen.getActivationStatus()).isEqualTo(ActivationStatus.ACTIVATED);
        verify(screenRepository).save(screen);
    }

    @Test
    void activate_invalidCode_throws400() {
        Screen screen = new Screen();
        screen.setActivationStatus(ActivationStatus.PENDING);
        screen.setActivationCode("RIGHT");

        when(screenRepository.findByDeviceCode("dev1")).thenReturn(Optional.of(screen));

        DeviceActivateRequest req = new DeviceActivateRequest();
        req.setDeviceCode("dev1");
        req.setActivationCode("WRONG");

        assertThatThrownBy(() -> deviceService.activate(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
    }

    @Test
    void activate_screenNotFound_throws404() {
        when(screenRepository.findByDeviceCode("missing")).thenReturn(Optional.empty());

        DeviceActivateRequest req = new DeviceActivateRequest();
        req.setDeviceCode("missing");
        req.setActivationCode("X");

        assertThatThrownBy(() -> deviceService.activate(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void activate_notPending_throws400() {
        Screen screen = new Screen();
        screen.setActivationStatus(ActivationStatus.ACTIVATED);
        when(screenRepository.findByDeviceCode("dev1")).thenReturn(Optional.of(screen));

        DeviceActivateRequest req = new DeviceActivateRequest();
        req.setDeviceCode("dev1");
        req.setActivationCode("ABC");

        assertThatThrownBy(() -> deviceService.activate(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
    }

    @Test
    void getActiveConfig_success() {
        Organization org = new Organization();
        org.setId(10L);
        Screen screen = new Screen();
        screen.setId(9L);
        screen.setOrganization(org);
        ActiveConfigResponse config = ActiveConfigResponse.builder().scheduleId(1L).build();

        when(screenRepository.fetchForResolve(9L)).thenReturn(Optional.of(screen));
        when(activeConfigService.resolve(any(Screen.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(config));

        assertThat(deviceService.getActiveConfig().getScheduleId()).isEqualTo(1L);
    }

    @Test
    void submitPlaybackLogs_success() {
        Organization org = new Organization();
        org.setId(10L);
        Screen screen = new Screen();
        screen.setId(9L);
        screen.setOrganization(org);
        Media media = new Media();
        media.setId(50L);
        Playlist playlist = new Playlist();
        playlist.setId(7L);

        when(screenRepository.findById(9L)).thenReturn(Optional.of(screen));
        when(mediaRepository.findByIdAndOrganization_Id(50L, 10L)).thenReturn(Optional.of(media));
        when(playlistRepository.findByIdAndOrganization_Id(7L, 10L)).thenReturn(Optional.of(playlist));

        PlaybackLogSubmitRequest.Entry entry = new PlaybackLogSubmitRequest.Entry();
        entry.setMediaId(50L);
        entry.setPlaylistId(7L);
        entry.setPlayedAt(LocalDateTime.now());
        PlaybackLogSubmitRequest request = new PlaybackLogSubmitRequest();
        request.setEntries(List.of(entry));

        deviceService.submitPlaybackLogs(request);

        verify(playbackLogRepository).save(any());
    }

    @Test
    void submitPlaybackLogs_withSchedule_success() {
        Organization org = new Organization();
        org.setId(10L);
        Screen screen = new Screen();
        screen.setId(9L);
        screen.setOrganization(org);
        Media media = new Media();
        media.setId(50L);
        Playlist playlist = new Playlist();
        playlist.setId(7L);
        Schedule schedule = new Schedule();
        schedule.setId(100L);

        when(screenRepository.findById(9L)).thenReturn(Optional.of(screen));
        when(mediaRepository.findByIdAndOrganization_Id(50L, 10L)).thenReturn(Optional.of(media));
        when(playlistRepository.findByIdAndOrganization_Id(7L, 10L)).thenReturn(Optional.of(playlist));
        when(scheduleRepository.findByIdAndOrganization_Id(100L, 10L)).thenReturn(Optional.of(schedule));

        PlaybackLogSubmitRequest.Entry entry = new PlaybackLogSubmitRequest.Entry();
        entry.setMediaId(50L);
        entry.setPlaylistId(7L);
        entry.setScheduleId(100L);
        entry.setPlayedAt(LocalDateTime.now());
        PlaybackLogSubmitRequest request = new PlaybackLogSubmitRequest();
        request.setEntries(List.of(entry));

        deviceService.submitPlaybackLogs(request);

        verify(playbackLogRepository).save(any());
    }
}
