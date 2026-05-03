package com.digitalsignage.admin.entity;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.common.enums.ScreenStatus;
import com.digitalsignage.admin.common.enums.WsStatus;
import com.digitalsignage.admin.entity.base.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "screen")
public class Screen extends BaseAuditableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_group_id")
    private ScreenGroup screenGroup;

    @Column(name = "device_code", nullable = false, unique = true, length = 64)
    private String deviceCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "activation_code", length = 128)
    private String activationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "activation_status", nullable = false, length = 32)
    private ActivationStatus activationStatus;

    @Column(name = "device_token", length = 512)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScreenStatus status;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "app_version", length = 64)
    private String appVersion;

    @Column(name = "resolution_width")
    private Integer resolutionWidth;

    @Column(name = "resolution_height")
    private Integer resolutionHeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "ws_status", nullable = false, length = 32)
    private WsStatus wsStatus;

    @Column(name = "last_ws_connected_at")
    private LocalDateTime lastWsConnectedAt;

    @Column(name = "last_ws_message_at")
    private LocalDateTime lastWsMessageAt;

    @Column(name = "probe_fail_count", nullable = false)
    private Integer probeFailCount;

    @PrePersist
    void applyDefaults() {
        if (probeFailCount == null) {
            probeFailCount = 0;
        }
        if (status == null) {
            status = ScreenStatus.OFFLINE;
        }
        if (wsStatus == null) {
            wsStatus = WsStatus.DISCONNECTED;
        }
        if (activationStatus == null) {
            activationStatus = ActivationStatus.PENDING;
        }
    }
}
