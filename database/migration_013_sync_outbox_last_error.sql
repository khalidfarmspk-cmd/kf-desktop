-- Migration 013: store last API error on sync_outbox rows for push diagnostics.
-- Run manually against local (MariaDB) and cloud (MySQL 9).
-- Back up first. Do NOT run from the app.

START TRANSACTION;

ALTER TABLE sync_outbox ADD COLUMN last_error TEXT NULL;

COMMIT;
