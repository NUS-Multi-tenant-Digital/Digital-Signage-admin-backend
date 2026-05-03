package com.digitalsignage.admin.entity;

import com.digitalsignage.admin.entity.base.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "layout_region")
public class LayoutRegion extends BaseAuditableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_id", nullable = false)
    private Layout layout;

    @Column(name = "region_name", nullable = false, length = 128)
    private String regionName;

    @Column(nullable = false)
    private Integer x;

    @Column(nullable = false)
    private Integer y;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(name = "z_index", nullable = false)
    private Integer zIndex;

    @Column(name = "component_type", nullable = false, length = 64)
    private String componentType;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;
}
