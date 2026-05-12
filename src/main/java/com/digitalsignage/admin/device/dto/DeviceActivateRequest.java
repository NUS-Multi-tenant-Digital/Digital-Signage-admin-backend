package com.digitalsignage.admin.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceActivateRequest {

    @NotBlank
    private String deviceCode;

    @NotBlank
    private String activationCode;
}
