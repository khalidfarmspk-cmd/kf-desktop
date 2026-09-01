-- Auth hardening: longer password hashes + unique usernames.
-- Run manually in phpMyAdmin / mysql CLI against database `pointofsale`.
-- Do not auto-run from the app.

ALTER TABLE users MODIFY password_user VARCHAR(255) NOT NULL;
ALTER TABLE users ADD UNIQUE KEY uk_username (username_user);
