-- Migration 005: invoice logo + barcode settings; multi-line footer storage.
-- Run manually against the existing pointofsale database.
-- Do not auto-run from the app. Back up first.

START TRANSACTION;

INSERT INTO pengaturan (setting_key, setting_value) VALUES
  ('logo_path', ''),
  ('show_logo', '1'),
  ('show_invoice_barcode', '1')
ON DUPLICATE KEY UPDATE setting_key = setting_key;

ALTER TABLE pengaturan MODIFY setting_value TEXT NULL;

COMMIT;
