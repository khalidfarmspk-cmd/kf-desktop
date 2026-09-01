-- =============================================================================
-- cloud_catchup.sql
-- Catch-up for Railway cloud MySQL (schema currently at migration 003).
-- Applies migrations 004 + 005 + 006 + 007 in order, idempotently.
--
-- Safe to re-run from the top after a partial apply:
-- CREATE TABLE IF NOT EXISTS, INSERT ... ON DUPLICATE KEY UPDATE,
-- and guarded foreign keys. ADD COLUMN has no IF NOT EXISTS
-- (MySQL — not MariaDB); cloud has never had these columns.
-- Do NOT reference a database name — run while connected to `railway`.
-- =============================================================================

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- 004 — pelanggan
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pelanggan (
  pelanggan_Id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nama_pelanggan VARCHAR(60) NOT NULL,
  telp_pelanggan VARCHAR(20) NULL,
  alamat_pelanggan VARCHAR(255) NULL,
  uuid CHAR(36) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
             ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pelanggan_uuid (uuid),
  KEY idx_pelanggan_telp (telp_pelanggan)
);

-- ---------------------------------------------------------------------------
-- 004 — metode_bayar + seed rows
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metode_bayar (
  metode_Id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nama_metode VARCHAR(30) NOT NULL,
  aktif TINYINT(1) NOT NULL DEFAULT 1,
  uuid CHAR(36) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
             ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_metode_nama (nama_metode),
  UNIQUE KEY uk_metode_uuid (uuid)
);

INSERT INTO metode_bayar (nama_metode, aktif, uuid) VALUES
  ('Cash', 1, UUID()),
  ('Card', 1, UUID()),
  ('Bank Transfer', 1, UUID()),
  ('Easypaisa', 1, UUID()),
  ('JazzCash', 1, UUID())
ON DUPLICATE KEY UPDATE aktif = VALUES(aktif);

-- ---------------------------------------------------------------------------
-- 004 — penjualan columns (customers, payment, bill discount)
-- ---------------------------------------------------------------------------
ALTER TABLE penjualan
  ADD COLUMN pelanggan_Id INT(11) NULL,
  ADD COLUMN metode_Id INT(11) NULL,
  ADD COLUMN subtotal_kotor INT(11) NOT NULL DEFAULT 0,
  ADD COLUMN diskon INT(11) NOT NULL DEFAULT 0;

-- Foreign keys (skip if already present)
SET @db := DATABASE();

SET @exist := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = @db
    AND TABLE_NAME = 'penjualan'
    AND CONSTRAINT_NAME = 'fk_penjualan_pelanggan'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql := IF(@exist = 0,
  'ALTER TABLE penjualan ADD CONSTRAINT fk_penjualan_pelanggan FOREIGN KEY (pelanggan_Id) REFERENCES pelanggan(pelanggan_Id)',
  'SELECT ''fk_penjualan_pelanggan already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = @db
    AND TABLE_NAME = 'penjualan'
    AND CONSTRAINT_NAME = 'fk_penjualan_metode'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql := IF(@exist = 0,
  'ALTER TABLE penjualan ADD CONSTRAINT fk_penjualan_metode FOREIGN KEY (metode_Id) REFERENCES metode_bayar(metode_Id)',
  'SELECT ''fk_penjualan_metode already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill gross for rows that predate bill discount (harmless if already set)
UPDATE penjualan
SET subtotal_kotor = Total_pembayaran
WHERE subtotal_kotor = 0;

-- ---------------------------------------------------------------------------
-- 004 — pengeluaran
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pengeluaran (
  pengeluaran_Id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tanggal DATE NOT NULL,
  kategori VARCHAR(40) NOT NULL,
  keterangan VARCHAR(255) NULL,
  jumlah INT(11) NOT NULL,
  user_Id INT(11) NOT NULL,
  uuid CHAR(36) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
             ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pengeluaran_uuid (uuid),
  KEY idx_pengeluaran_tanggal (tanggal)
);

SET @exist := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = @db
    AND TABLE_NAME = 'pengeluaran'
    AND CONSTRAINT_NAME = 'fk_pengeluaran_user'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql := IF(@exist = 0,
  'ALTER TABLE pengeluaran ADD CONSTRAINT fk_pengeluaran_user FOREIGN KEY (user_Id) REFERENCES users(user_Id)',
  'SELECT ''fk_pengeluaran_user already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 004 — pengaturan + seed rows
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pengaturan (
  setting_key VARCHAR(50) NOT NULL PRIMARY KEY,
  setting_value VARCHAR(500) NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
             ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO pengaturan (setting_key, setting_value) VALUES
  ('max_discount_percent', '10'),
  ('shop_name', 'Khalid Farms'),
  ('shop_address', 'Bahria town Lahore'),
  ('shop_phone', ''),
  ('logo_path', 'data/shop_logo.jpg'),
  ('show_logo', '1'),
  ('receipt_footer', 'Thank you'),
  ('show_customer', '1'),
  ('show_cashier', '1'),
  ('show_payment_method', '1')
ON DUPLICATE KEY UPDATE setting_key = setting_key;

-- ---------------------------------------------------------------------------
-- 005 — invoice logo / barcode settings; widen setting_value
-- ---------------------------------------------------------------------------
INSERT INTO pengaturan (setting_key, setting_value) VALUES
  ('logo_path', ''),
  ('show_logo', '1'),
  ('show_invoice_barcode', '1')
ON DUPLICATE KEY UPDATE setting_key = setting_key;

ALTER TABLE pengaturan MODIFY setting_value TEXT NULL;

-- ---------------------------------------------------------------------------
-- 006 — delivery payment methods
-- ---------------------------------------------------------------------------
INSERT INTO metode_bayar (nama_metode, aktif, uuid) VALUES
  ('Cash on Delivery', 1, UUID()),
  ('Card on Delivery', 1, UUID())
ON DUPLICATE KEY UPDATE aktif = VALUES(aktif);

-- ---------------------------------------------------------------------------
-- 007 — delivery man name on sales
-- ---------------------------------------------------------------------------
ALTER TABLE penjualan
  ADD COLUMN nama_kurir VARCHAR(60) NULL AFTER metode_Id;

COMMIT;
