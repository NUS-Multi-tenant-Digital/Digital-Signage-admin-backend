package com.digitalsignage.admin.entity;

import com.digitalsignage.admin.common.enums.LayoutStatus;
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
@Table(name = "layout")
public class Layout extends BaseAuditableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Column(name = "template_type", nullable = false, length = 64)
    private String templateType;

    @Column(name = "resolution_width", nullable = false)
    private Integer resolutionWidth;

    @Column(name = "resolution_height", nullable = false)
    private Integer resolutionHeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LayoutStatus status;
}
