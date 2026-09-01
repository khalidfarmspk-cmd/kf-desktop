-- Migration 009: local sync outbox + sync state for cloud background sync.
-- Run manually against local (MariaDB) and cloud (MySQL 9).
-- Do not use ADD COLUMN IF NOT EXISTS (MariaDB-only).
-- Back up first.
-- Do NOT run from the app — apply with mysql client / DBeaver.

START TRANSACTION;

CREATE TABLE sync_outbox (
  outbox_Id    INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  entity_type  VARCHAR(30) NOT NULL,
  entity_uuid  CHAR(36) NOT NULL,
  payload      TEXT NOT NULL,
  created_at   TIMESTAMP NOT NULL
               DEFAULT CURRENT_TIMESTAMP,
  attempts     INT(11) NOT NULL DEFAULT 0,
  last_attempt TIMESTAMP NULL,
  UNIQUE KEY uk_outbox_uuid (entity_uuid)
);

CREATE TABLE sync_state (
  state_key    VARCHAR(50) NOT NULL PRIMARY KEY,
  state_value  VARCHAR(500) NULL,
  updated_at   TIMESTAMP NOT NULL
               DEFAULT CURRENT_TIMESTAMP
               ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO sync_state (state_key, state_value) VALUES
  ('last_pull_at', NULL),
  ('api_base_url', 'https://pos-api-production-91dc.up.railway.app'),
  ('api_token', NULL)
ON DUPLICATE KEY UPDATE state_key = state_key;

INSERT INTO pengaturan (setting_key, setting_value) VALUES
  ('sync_enabled', '0')
ON DUPLICATE KEY UPDATE setting_key = setting_key;

COMMIT;
