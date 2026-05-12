package com.digitalsignage.admin.screen.service;

import com.digitalsignage.admin.screen.dto.DeviceEventLogResponse;
import com.digitalsignage.admin.screen.dto.PlaybackLogAdminResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScreenLogService {

    Page<DeviceEventLogResponse> listEvents(Long screenId, Pageable pageable);

    Page<PlaybackLogAdminResponse> listPlaybackLogs(Long screenId, Pageable pageable);
}
