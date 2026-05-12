-- One layout region can hold multiple components (stacked / ordered within the region).

CREATE TABLE IF NOT EXISTS layout_region_component (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    layout_region_id BIGINT NOT NULL,
    component_type VARCHAR(64) NOT NULL,
    config_json TEXT NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_layout_region_component_region FOREIGN KEY (layout_region_id) REFERENCES layout_region (id) ON DELETE CASCADE
);

INSERT INTO layout_region_component (created_at, updated_at, layout_region_id, component_type, config_json, sort_order)
SELECT lr.created_at, lr.updated_at, lr.id, lr.component_type, lr.config_json, 0
FROM layout_region lr;

ALTER TABLE layout_region DROP COLUMN component_type;
ALTER TABLE layout_region DROP COLUMN config_json;

CREATE INDEX idx_layout_region_component_region_id ON layout_region_component (layout_region_id);
