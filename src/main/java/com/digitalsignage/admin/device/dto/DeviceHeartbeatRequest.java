package com.digitalsignage.admin.device.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceHeartbeatRequest {

    /**
     * 运行是否正常；false 表示连接仍在但业务异常（对应 Modules「ERROR」状态）。
     */
    private Boolean runtimeHealthy;

    private String appVersion;
    private Integer resolutionWidth;
    private Integer resolutionHeight;
}
