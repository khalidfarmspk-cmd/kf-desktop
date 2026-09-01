-- Migration 010: uuid + updated_at on users for cloud↔shop user pull.
-- Run manually against local (MariaDB) and cloud (MySQL 9).
-- Do not use ADD COLUMN IF NOT EXISTS (MariaDB-only).
-- Back up first.
-- Do NOT run from the app.

START TRANSACTION;

ALTER TABLE users ADD COLUMN uuid CHAR(36) NULL;
UPDATE users SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE users MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE users ADD UNIQUE KEY uk_users_uuid (uuid);

ALTER TABLE users
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP
  ON UPDATE CURRENT_TIMESTAMP;

COMMIT;
