package com.digitalsignage.admin.entity;

import com.digitalsignage.admin.common.enums.DeviceEventLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "device_event_log")
@EntityListeners(AuditingEntityListener.class)
public class DeviceEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_level", nullable = false, length = 32)
    private DeviceEventLevel eventLevel;

    @Column(length = 2048)
    private String message;

    @Column(name = "event_id", unique = true, length = 64)
    private String eventId;

    @Column(name = "manifest_id", length = 64)
    private String manifestId;

    @Column(name = "manifest_version")
    private Long manifestVersion;

    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "playlist_item_id", length = 64)
    private String playlistItemId;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "extra_json", columnDefinition = "TEXT")
    private String extraJson;

    @Column(name = "event_timestamp")
    private Long eventTimestamp;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
