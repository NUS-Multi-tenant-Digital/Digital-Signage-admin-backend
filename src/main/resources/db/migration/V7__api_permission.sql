CREATE TABLE api_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(128) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    path_pattern VARCHAR(512) NOT NULL,
    principal_type VARCHAR(16) NOT NULL,
    access_mode VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 100,
    description VARCHAR(512) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_api_permission_code UNIQUE (code)
);

CREATE TABLE role_api_permission (
    role VARCHAR(32) NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role, permission_id),
    CONSTRAINT fk_role_api_permission FOREIGN KEY (permission_id) REFERENCES api_permission (id)
);

-- Admin: user management (higher priority = lower sort_order)
INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:users:create', 'POST', '/api/admin/users', 'ADMIN', 'ROLE_SCOPED', 20, 'Create users');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:users:update', 'PUT', '/api/admin/users/*', 'ADMIN', 'ROLE_SCOPED', 21, 'Update user');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:users:delete', 'DELETE', '/api/admin/users/*', 'ADMIN', 'ROLE_SCOPED', 22, 'Delete user');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:users:read', 'GET', '/api/admin/users', 'ADMIN', 'ROLE_SCOPED', 30, 'List users');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:users:read-by-id', 'GET', '/api/admin/users/*', 'ADMIN', 'ROLE_SCOPED', 31, 'Get user or /me');

-- Admin: media
INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:media:write', 'POST', '/api/admin/media/*', 'ADMIN', 'ROLE_SCOPED', 40, 'Upload media');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:media:delete', 'DELETE', '/api/admin/media/*', 'ADMIN', 'ROLE_SCOPED', 41, 'Delete media');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:media:read', 'GET', '/api/admin/media', 'ADMIN', 'ROLE_SCOPED', 42, 'List media');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:media:read-by-id', 'GET', '/api/admin/media/*', 'ADMIN', 'ROLE_SCOPED', 43, 'Get media');

-- Admin: general write (screens, layouts, schedules, playlists, etc.)
INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:write:post', 'POST', '/api/admin/**', 'ADMIN', 'ROLE_SCOPED', 50, 'Admin POST');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:write:put', 'PUT', '/api/admin/**', 'ADMIN', 'ROLE_SCOPED', 51, 'Admin PUT');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:write:patch', 'PATCH', '/api/admin/**', 'ADMIN', 'ROLE_SCOPED', 52, 'Admin PATCH');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:write:delete', 'DELETE', '/api/admin/**', 'ADMIN', 'ROLE_SCOPED', 53, 'Admin DELETE');

-- Admin: catch-all read for any authenticated role
INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('admin:read:any', 'GET', '/api/admin/**', 'ADMIN', 'AUTHENTICATED', 900, 'Admin GET catch-all');

-- Device APIs
INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('device:active-config', 'GET', '/api/device/active-config', 'DEVICE', 'ROLE_SCOPED', 10, 'Pull active config');

INSERT INTO api_permission (code, http_method, path_pattern, principal_type, access_mode, sort_order, description)
VALUES ('device:playback-logs', 'POST', '/api/device/playback-logs', 'DEVICE', 'ROLE_SCOPED', 11, 'Submit playback logs');

-- role_api_permission: ADMIN-only user mutations
INSERT INTO role_api_permission (role, permission_id)
SELECT 'ADMIN', id FROM api_permission WHERE code IN ('admin:users:create', 'admin:users:update', 'admin:users:delete');

INSERT INTO role_api_permission (role, permission_id)
SELECT 'ADMIN', id FROM api_permission WHERE code IN ('admin:users:read', 'admin:users:read-by-id');

INSERT INTO role_api_permission (role, permission_id)
SELECT 'EDITOR', id FROM api_permission WHERE code IN ('admin:users:read', 'admin:users:read-by-id');

INSERT INTO role_api_permission (role, permission_id)
SELECT 'VIEWER', id FROM api_permission WHERE code IN ('admin:users:read', 'admin:users:read-by-id');

-- media: ADMIN + EDITOR write/delete; all roles read
INSERT INTO role_api_permission (role, permission_id)
SELECT 'ADMIN', id FROM api_permission WHERE code IN ('admin:media:write', 'admin:media:delete');

INSERT INTO role_api_permission (role, permission_id)
SELECT 'EDITOR', id FROM api_permission WHERE code IN ('admin:media:write', 'admin:media:delete');

INSERT INTO role_api_permission (role, permission_id)
SELECT r.role, p.id
FROM api_permission p
CROSS JOIN (SELECT 'ADMIN' AS role UNION SELECT 'EDITOR' UNION SELECT 'VIEWER') r
WHERE p.code IN ('admin:media:read', 'admin:media:read-by-id');

-- general admin write: ADMIN + EDITOR
INSERT INTO role_api_permission (role, permission_id)
SELECT 'ADMIN', id FROM api_permission WHERE code LIKE 'admin:write:%';

INSERT INTO role_api_permission (role, permission_id)
SELECT 'EDITOR', id FROM api_permission WHERE code LIKE 'admin:write:%';

-- device
INSERT INTO role_api_permission (role, permission_id)
SELECT 'DEVICE', id FROM api_permission WHERE principal_type = 'DEVICE';
