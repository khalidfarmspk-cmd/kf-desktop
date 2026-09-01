-- Migration 004: customers, payment methods, bill-level discount,
-- expenses, and invoice settings.
-- Run manually against the existing pointofsale database.
-- Do not auto-run from the app. Back up first.

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- CUSTOMERS (separate from users — customers have no login)
-- ---------------------------------------------------------------------------
CREATE TABLE pelanggan (
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
-- PAYMENT METHODS
-- ---------------------------------------------------------------------------
CREATE TABLE metode_bayar (
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
  ('JazzCash', 1, UUID());

-- ---------------------------------------------------------------------------
-- SALE CHANGES — discount is on the whole bill, not per line
-- subtotal_kotor = sum of lines before discount
-- diskon         = discount amount in rupees
-- Total_pembayaran = subtotal_kotor - diskon (what the customer paid)
-- ---------------------------------------------------------------------------
ALTER TABLE penjualan
  ADD COLUMN pelanggan_Id INT(11) NULL,
  ADD COLUMN metode_Id INT(11) NULL,
  ADD COLUMN subtotal_kotor INT(11) NOT NULL DEFAULT 0,
  ADD COLUMN diskon INT(11) NOT NULL DEFAULT 0;

ALTER TABLE penjualan
  ADD CONSTRAINT fk_penjualan_pelanggan
    FOREIGN KEY (pelanggan_Id) REFERENCES pelanggan(pelanggan_Id),
  ADD CONSTRAINT fk_penjualan_metode
    FOREIGN KEY (metode_Id) REFERENCES metode_bayar(metode_Id);

UPDATE penjualan SET subtotal_kotor = Total_pembayaran
WHERE subtotal_kotor = 0;

-- ---------------------------------------------------------------------------
-- EXPENSES
-- ---------------------------------------------------------------------------
CREATE TABLE pengeluaran (
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
  KEY idx_pengeluaran_tanggal (tanggal),
  CONSTRAINT fk_pengeluaran_user
    FOREIGN KEY (user_Id) REFERENCES users(user_Id)
);

-- ---------------------------------------------------------------------------
-- SETTINGS — discount cap and invoice layout
-- ---------------------------------------------------------------------------
CREATE TABLE pengaturan (
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
  ('show_payment_method', '1');

COMMIT;
