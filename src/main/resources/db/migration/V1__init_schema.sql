CREATE TABLE IF NOT EXISTS organization (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_organization_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS screen_group (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    organization_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(512) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_screen_group_organization FOREIGN KEY (organization_id) REFERENCES organization (id)
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    organization_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sys_user_organization FOREIGN KEY (organization_id) REFERENCES organization (id)
);

CREATE TABLE IF NOT EXISTS layout (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    organization_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    template_type VARCHAR(64) NOT NULL,
    resolution_width INT NOT NULL,
    resolution_height INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_layout_organization FOREIGN KEY (organization_id) REFERENCES organization (id)
);

CREATE TABLE IF NOT EXISTS layout_region (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    layout_id BIGINT NOT NULL,
    region_name VARCHAR(128) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    z_index INT NOT NULL,
    component_type VARCHAR(64) NOT NULL,
    config_json TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_layout_region_layout FOREIGN KEY (layout_id) REFERENCES layout (id)
);

CREATE TABLE IF NOT EXISTS media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    organization_id BIGINT NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    file_url VARCHAR(1024) NULL,
    thumbnail_url VARCHAR(1024) NULL,
    file_size_bytes BIGINT NULL,
    duration_seconds INT NULL,
    checksum_sha256 VARCHAR(64) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_media_organization FOREIGN KEY (organization_id) REFERENCES organization (id)
);

CREATE TABLE IF NOT EXISTS playlist (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    organization_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_playlist_organization FOREIGN KEY (organization_id) REFERENCES organization (id)
);

CREATE TABLE IF NOT EXISTS screen (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    organization_id BIGINT NOT NULL,
    screen_group_id BIGINT NULL,
    device_code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    activation_code VARCHAR(128) NULL,
    activation_status VARCHAR(32) NOT NULL,
    device_token VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL,
    last_heartbeat_at DATETIME(6) NULL,
    app_version VARCHAR(64) NULL,
    resolution_width INT NULL,
    resolution_height INT NULL,
    ws_status VARCHAR(32) NOT NULL,
    last_ws_connected_at DATETIME(6) NULL,
    last_ws_message_at DATETIME(6) NULL,
    probe_fail_count INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_screen_device_code UNIQUE (device_code),
    CONSTRAINT fk_screen_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT fk_screen_group FOREIGN KEY (screen_group_id) REFERENCES screen_group (id)
);

CREATE TABLE IF NOT EXISTS schedule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    organization_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    screen_id BIGINT NULL,
    screen_group_id BIGINT NULL,
    layout_id BIGINT NOT NULL,
    playlist_id BIGINT NOT NULL,
    start_datetime DATETIME(6) NOT NULL,
    end_datetime DATETIME(6) NOT NULL,
    priority INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_schedule_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT fk_schedule_screen FOREIGN KEY (screen_id) REFERENCES screen (id),
    CONSTRAINT fk_schedule_screen_group FOREIGN KEY (screen_group_id) REFERENCES screen_group (id),
    CONSTRAINT fk_schedule_layout FOREIGN KEY (layout_id) REFERENCES layout (id),
    CONSTRAINT fk_schedule_playlist FOREIGN KEY (playlist_id) REFERENCES playlist (id)
);

CREATE TABLE IF NOT EXISTS playlist_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    playlist_id BIGINT NOT NULL,
    media_id BIGINT NOT NULL,
    order_index INT NOT NULL,
    duration_seconds INT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_playlist_item_order UNIQUE (playlist_id, order_index),
    CONSTRAINT fk_playlist_item_playlist FOREIGN KEY (playlist_id) REFERENCES playlist (id),
    CONSTRAINT fk_playlist_item_media FOREIGN KEY (media_id) REFERENCES media (id)
);

CREATE TABLE IF NOT EXISTS device_event_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    screen_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_level VARCHAR(32) NOT NULL,
    message VARCHAR(2048) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_device_event_log_screen FOREIGN KEY (screen_id) REFERENCES screen (id)
);

CREATE TABLE IF NOT EXISTS playback_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    screen_id BIGINT NOT NULL,
    media_id BIGINT NOT NULL,
    playlist_id BIGINT NOT NULL,
    schedule_id BIGINT NULL,
    played_at DATETIME(6) NOT NULL,
    duration_played INT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_playback_log_screen FOREIGN KEY (screen_id) REFERENCES screen (id),
    CONSTRAINT fk_playback_log_media FOREIGN KEY (media_id) REFERENCES media (id),
    CONSTRAINT fk_playback_log_playlist FOREIGN KEY (playlist_id) REFERENCES playlist (id),
    CONSTRAINT fk_playback_log_schedule FOREIGN KEY (schedule_id) REFERENCES schedule (id)
);

CREATE INDEX idx_screen_group_organization_id ON screen_group (organization_id);
CREATE INDEX idx_sys_user_organization_id ON sys_user (organization_id);
CREATE INDEX idx_layout_organization_id ON layout (organization_id);
CREATE INDEX idx_layout_region_layout_id ON layout_region (layout_id);
CREATE INDEX idx_media_organization_id ON media (organization_id);
CREATE INDEX idx_playlist_organization_id ON playlist (organization_id);
CREATE INDEX idx_screen_organization_id ON screen (organization_id);
CREATE INDEX idx_screen_screen_group_id ON screen (screen_group_id);
CREATE INDEX idx_schedule_organization_id ON schedule (organization_id);
CREATE INDEX idx_schedule_screen_id ON schedule (screen_id);
CREATE INDEX idx_schedule_screen_group_id ON schedule (screen_group_id);
CREATE INDEX idx_schedule_layout_id ON schedule (layout_id);
CREATE INDEX idx_schedule_playlist_id ON schedule (playlist_id);
CREATE INDEX idx_playlist_item_playlist_id ON playlist_item (playlist_id);
CREATE INDEX idx_playlist_item_media_id ON playlist_item (media_id);
CREATE INDEX idx_device_event_log_screen_id ON device_event_log (screen_id);
CREATE INDEX idx_playback_log_screen_id ON playback_log (screen_id);
CREATE INDEX idx_playback_log_media_id ON playback_log (media_id);
CREATE INDEX idx_playback_log_playlist_id ON playback_log (playlist_id);
CREATE INDEX idx_playback_log_schedule_id ON playback_log (schedule_id);
