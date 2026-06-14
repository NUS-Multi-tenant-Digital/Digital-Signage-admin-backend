package com.digitalsignage.admin.emergency.service;

import com.digitalsignage.admin.emergency.dto.ActivateEmergencyOverrideRequest;
import com.digitalsignage.admin.emergency.dto.EmergencyOverrideResponse;

import java.util.List;

public interface EmergencyOverrideService {

    EmergencyOverrideResponse activate(ActivateEmergencyOverrideRequest request);

    void cancel(Long scheduleId);

    List<EmergencyOverrideResponse> listActive();
}
