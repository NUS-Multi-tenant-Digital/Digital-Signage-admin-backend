package com.digitalsignage.admin.screen.service.impl;

import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.device.repository.DeviceEventLogRepository;
import com.digitalsignage.admin.device.repository.PlaybackLogRepository;
import com.digitalsignage.admin.screen.dto.DeviceEventLogResponse;
import com.digitalsignage.admin.screen.dto.PlaybackLogAdminResponse;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.screen.service.ScreenLogService;
import com.digitalsignage.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScreenLogServiceImpl implements ScreenLogService {

    private final ScreenRepository screenRepository;
    private final DeviceEventLogRepository deviceEventLogRepository;
    private final PlaybackLogRepository playbackLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DeviceEventLogResponse> listEvents(Long screenId, Pageable pageable) {
        assertScreenInTenant(screenId);
        return deviceEventLogRepository.findByScreen_IdOrderByCreatedAtDesc(screenId, pageable)
                .map(DeviceEventLogResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlaybackLogAdminResponse> listPlaybackLogs(Long screenId, Pageable pageable) {
        assertScreenInTenant(screenId);
        return playbackLogRepository.findByScreen_IdOrderByPlayedAtDesc(screenId, pageable)
                .map(PlaybackLogAdminResponse::fromEntity);
    }

    private void assertScreenInTenant(Long screenId) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        screenRepository.findByIdAndOrganization_Id(screenId, organizationId)
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
    }
}
