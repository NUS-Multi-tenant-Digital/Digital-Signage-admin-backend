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
@Table(name = "layout_region_component")
public class LayoutRegionComponent extends BaseAuditableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_region_id", nullable = false)
    private LayoutRegion region;

    @Column(name = "component_type", nullable = false, length = 64)
    private String componentType;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
