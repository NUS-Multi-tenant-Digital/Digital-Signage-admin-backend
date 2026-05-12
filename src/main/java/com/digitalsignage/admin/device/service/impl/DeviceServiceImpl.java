package com.digitalsignage.admin.device.service.impl;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.device.dto.DeviceActivateRequest;
import com.digitalsignage.admin.device.dto.DeviceActivateResponse;
import com.digitalsignage.admin.device.dto.PlaybackLogSubmitRequest;
import com.digitalsignage.admin.device.repository.PlaybackLogRepository;
import com.digitalsignage.admin.device.service.ActiveConfigService;
import com.digitalsignage.admin.device.service.DeviceService;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.PlaybackLog;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.DevicePrincipal;
import com.digitalsignage.admin.security.SecurityUtils;
import com.digitalsignage.admin.common.util.RandomCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final ScreenRepository screenRepository;
    private final ActiveConfigService activeConfigService;
    private final MediaRepository mediaRepository;
    private final PlaylistRepository playlistRepository;
    private final ScheduleRepository scheduleRepository;
    private final PlaybackLogRepository playbackLogRepository;

    @Override
    @Transactional
    public DeviceActivateResponse activate(DeviceActivateRequest request) {
        Screen screen = screenRepository.findByDeviceCode(request.getDeviceCode().trim())
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
        if (screen.getActivationStatus() != ActivationStatus.PENDING) {
            throw new BusinessException(400, "activation not allowed");
        }
        if (!Objects.equals(screen.getActivationCode(), request.getActivationCode())) {
            throw new BusinessException(400, "invalid activation code");
        }
        String token = RandomCodes.hexToken(32);
        screen.setDeviceToken(token);
        screen.setActivationStatus(ActivationStatus.ACTIVATED);
        screenRepository.save(screen);
        return DeviceActivateResponse.builder()
                .deviceToken(token)
                .screenId(screen.getId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveConfigResponse getActiveConfig() {
        DevicePrincipal device = SecurityUtils.requireDevice();
        Screen screen = screenRepository.fetchForResolve(device.getScreenId())
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
        return activeConfigService.resolve(screen, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(404, "no active configuration"));
    }

    @Override
    @Transactional
    public void submitPlaybackLogs(PlaybackLogSubmitRequest request) {
        DevicePrincipal device = SecurityUtils.requireDevice();
        Screen screen = screenRepository.findById(device.getScreenId())
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
        Long orgId = screen.getOrganization().getId();
        for (PlaybackLogSubmitRequest.Entry entry : request.getEntries()) {
            Media media = mediaRepository.findByIdAndOrganization_Id(entry.getMediaId(), orgId)
                    .orElseThrow(() -> new BusinessException(400, "media not found"));
            Playlist playlist = playlistRepository.findByIdAndOrganization_Id(entry.getPlaylistId(), orgId)
                    .orElseThrow(() -> new BusinessException(400, "playlist not found"));
            Schedule schedule = null;
            if (entry.getScheduleId() != null) {
                schedule = scheduleRepository.findByIdAndOrganization_Id(entry.getScheduleId(), orgId)
                        .orElseThrow(() -> new BusinessException(400, "schedule not found"));
            }
            PlaybackLog log = new PlaybackLog();
            log.setScreen(screen);
            log.setMedia(media);
            log.setPlaylist(playlist);
            log.setSchedule(schedule);
            log.setPlayedAt(entry.getPlayedAt());
            log.setDurationPlayed(entry.getDurationPlayed());
            playbackLogRepository.save(log);
        }
    }
}
