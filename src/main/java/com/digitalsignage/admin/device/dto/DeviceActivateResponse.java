package com.digitalsignage.admin.device.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviceActivateResponse {

    private String deviceToken;
    private Long screenId;
}
