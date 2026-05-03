package com.digitalsignage.admin.entity;

import com.digitalsignage.admin.common.enums.MediaType;
import com.digitalsignage.admin.entity.base.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "media")
public class Media extends BaseAuditableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 32)
    private MediaType mediaType;

    @Column(nullable = false)
    private String name;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "file_url", length = 1024)
    private String fileUrl;

    @Column(name = "thumbnail_url", length = 1024)
    private String thumbnailUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;
}
