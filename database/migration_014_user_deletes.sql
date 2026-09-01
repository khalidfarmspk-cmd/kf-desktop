-- Migration 014: tombstones so user deletions propagate in both directions.
-- A hard DELETE leaves nothing for /sync/changes to report, so each removal
-- records its uuid here and the other side replays it.
-- Run manually against local (MariaDB) and cloud (MySQL 9).
-- Back up first.
-- Do NOT run from the app.

START TRANSACTION;

CREATE TABLE deleted_users (
  uuid       CHAR(36) NOT NULL PRIMARY KEY,
  deleted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_deleted_users_at ON deleted_users (deleted_at);

COMMIT;
