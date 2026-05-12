package com.digitalsignage.admin.device.service.impl;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.device.dto.DeviceActivateRequest;
import com.digitalsignage.admin.device.dto.DeviceActivateResponse;
import com.digitalsignage.admin.device.repository.PlaybackLogRepository;
import com.digitalsignage.admin.device.service.ActiveConfigService;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
