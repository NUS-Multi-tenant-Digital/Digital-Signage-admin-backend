ALTER TABLE sys_user DROP INDEX idx_sys_user_email_verification_token;

ALTER TABLE sys_user DROP COLUMN email_verification_token;

ALTER TABLE sys_user DROP COLUMN email_verification_token_expires_at;
