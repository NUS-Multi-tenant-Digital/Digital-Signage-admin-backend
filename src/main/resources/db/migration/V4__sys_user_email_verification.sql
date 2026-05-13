ALTER TABLE sys_user ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE sys_user ADD COLUMN email_verification_token VARCHAR(64) NULL;

ALTER TABLE sys_user ADD COLUMN email_verification_token_expires_at DATETIME(6) NULL;

CREATE INDEX idx_sys_user_email_verification_token ON sys_user (email_verification_token);
