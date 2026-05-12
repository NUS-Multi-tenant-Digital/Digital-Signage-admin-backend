package com.digitalsignage.admin.device.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 保活与离线判定（对齐 docs/Modules.md「在线状态」语义）。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.device.presence")
public class DevicePresenceProperties {

    /**
     * 已认证 WS 会话下，超过该秒数未收到任何 WS 消息（含 PONG / STATUS_REPORT）则记一次探测失败。
     */
    private int wsSilenceThresholdSeconds = 90;

    /**
     * 连续探测失败达到该次数后标记为 OFFLINE 并清理会话认知（数据库状态）。
     */
    private int offlineProbeThreshold = 3;

    /**
     * 首次探测失败将 {@link com.digitalsignage.admin.common.enums.ScreenStatus} 置为 SUSPECT（由 ONLINE 降级）。
     */
    private int suspectProbeThreshold = 1;

    /**
     * HTTP 心跳距离现在超过该秒数且仍为 WS CONNECTED 时，参与将状态降为 SUSPECT（运行侧失联）。
     */
    private int heartbeatStaleSeconds = 600;
}
