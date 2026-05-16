-- Device APIs use device_token auth only; not managed via role_api_permission.

DELETE FROM role_api_permission WHERE role = 'DEVICE';

DELETE FROM api_permission WHERE principal_type = 'DEVICE';
