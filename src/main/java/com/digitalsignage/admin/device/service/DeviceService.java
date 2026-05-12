package com.digitalsignage.admin.device.service;

import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.device.dto.DeviceActivateRequest;
import com.digitalsignage.admin.device.dto.DeviceActivateResponse;
import com.digitalsignage.admin.device.dto.DeviceHeartbeatRequest;
import com.digitalsignage.admin.device.dto.PlaybackLogSubmitRequest;

public interface DeviceService {

    DeviceActivateResponse activate(DeviceActivateRequest request);

    ActiveConfigResponse getActiveConfig();

    void heartbeat(DeviceHeartbeatRequest request);

    void submitPlaybackLogs(PlaybackLogSubmitRequest request);
}
