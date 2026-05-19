-- Player Application Server tables
-- These tables support device-side manifest delivery, caching, heartbeat, commands and event logging.

CREATE TABLE IF NOT EXISTS player_configs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    screen_id BIGINT NOT NULL,
    heartbeat_interval_sec INT NOT NULL DEFAULT 30,
    manifest_sync_interval_sec INT NOT NULL DEFAULT 60,
    event_flush_interval_sec INT NOT NULL DEFAULT 30,
    max_cache_size_mb BIGINT NOT NULL DEFAULT 2048,
    asset_download_concurrency INT NOT NULL DEFAULT 3,
    enable_offline_mode TINYINT(1) NOT NULL DEFAULT 1,
    enable_watchdog TINYINT(1) NOT NULL DEFAULT 1,
    enable_screenshot TINYINT(1) NOT NULL DEFAULT 0,
    log_level VARCHAR(16) NOT NULL DEFAULT 'info',
    supported_asset_types_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_player_configs_screen_id (screen_id),
    CONSTRAINT fk_player_configs_screen FOREIGN KEY (screen_id) REFERENCES screen (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS manifests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    manifest_id VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL,
    screen_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    layout_id BIGINT NULL,
    valid_from BIGINT NOT NULL,
    valid_to BIGINT NOT NULL DEFAULT 0,
    ttl_sec INT NOT NULL DEFAULT 3600,
    template_config_json TEXT NOT NULL,
    playback_plan_json TEXT NOT NULL,
    cache_policy_json TEXT NOT NULL,
    fallback_policy_json TEXT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    generated_at BIGINT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_manifests_manifest_id (manifest_id),
    UNIQUE KEY uk_manifests_screen_version (screen_id, version),
    KEY idx_manifests_screen_active (screen_id, is_active),
    KEY idx_manifests_organization_id (organization_id),
    CONSTRAINT fk_manifests_screen FOREIGN KEY (screen_id) REFERENCES screen (id) ON DELETE CASCADE,
    CONSTRAINT fk_manifests_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT fk_manifests_layout FOREIGN KEY (layout_id) REFERENCES layout (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS manifest_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    manifest_id VARCHAR(64) NOT NULL,
    media_id BIGINT NOT NULL,
    required TINYINT(1) NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_manifest_media (manifest_id, media_id),
    KEY idx_manifest_media_media_id (media_id),
    CONSTRAINT fk_manifest_media_manifest FOREIGN KEY (manifest_id) REFERENCES manifests (manifest_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_manifest_media_media FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS commands (
    id BIGINT NOT NULL AUTO_INCREMENT,
    command_id VARCHAR(64) NOT NULL,
    screen_id BIGINT NOT NULL,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    payload_json TEXT NOT NULL,
    issued_at BIGINT NOT NULL,
    expire_at BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_commands_command_id (command_id),
    KEY idx_commands_screen_status (screen_id, status),
    KEY idx_commands_expire_at (expire_at),
    CONSTRAINT fk_commands_screen FOREIGN KEY (screen_id) REFERENCES screen (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS command_acks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    command_id VARCHAR(64) NOT NULL,
    screen_id BIGINT NOT NULL,
    type VARCHAR(64) NOT NULL,
    success TINYINT(1) NOT NULL,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(512) NULL,
    executed_at BIGINT NOT NULL,
    received_at BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_command_acks_command_id (command_id),
    KEY idx_command_acks_screen_id (screen_id),
    CONSTRAINT fk_command_acks_command FOREIGN KEY (command_id) REFERENCES commands (command_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_command_acks_screen FOREIGN KEY (screen_id) REFERENCES screen (id) ON DELETE CASCADE
);

-- Extend device_event_log with player-server fields
ALTER TABLE device_event_log ADD COLUMN event_id VARCHAR(64) NULL;
ALTER TABLE device_event_log ADD COLUMN manifest_id VARCHAR(64) NULL;
ALTER TABLE device_event_log ADD COLUMN manifest_version BIGINT NULL;
ALTER TABLE device_event_log ADD COLUMN media_id BIGINT NULL;
ALTER TABLE device_event_log ADD COLUMN playlist_item_id VARCHAR(64) NULL;
ALTER TABLE device_event_log ADD COLUMN error_code VARCHAR(64) NULL;
ALTER TABLE device_event_log ADD COLUMN error_message VARCHAR(512) NULL;
ALTER TABLE device_event_log ADD COLUMN extra_json TEXT NULL;
ALTER TABLE device_event_log ADD COLUMN event_timestamp BIGINT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_device_event_log_event_id ON device_event_log (event_id);
CREATE INDEX IF NOT EXISTS idx_device_event_log_screen_timestamp ON device_event_log (screen_id, event_timestamp);
