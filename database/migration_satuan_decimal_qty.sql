-- Migration: unit-of-measure (satuan) + fractional quantities
-- Run manually against the existing PointOfSale / pointofsale database.
-- Safe to review before applying. Back up first.

START TRANSACTION;

-- 1) Units table
CREATE TABLE IF NOT EXISTS satuan (
  satuan_Id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nama_satuan VARCHAR(20) NOT NULL,
  allow_decimal TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_nama_satuan (nama_satuan)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO satuan (nama_satuan, allow_decimal) VALUES
  ('Piece', 0),
  ('Dozen', 0),
  ('Packet', 0),
  ('Box', 0),
  ('Bottle', 0),
  ('Bag', 0),
  ('Kg', 1),
  ('Gram', 1),
  ('Litre', 1),
  ('ml', 1),
  ('Metre', 1)
ON DUPLICATE KEY UPDATE allow_decimal = VALUES(allow_decimal);

-- 2) Link products to units (default existing rows to Piece)
-- Skip ADD COLUMN if it already exists on a re-run.
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'produk'
    AND COLUMN_NAME = 'satuan_Id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE produk ADD COLUMN satuan_Id INT(11) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE produk
SET satuan_Id = (SELECT s.satuan_Id FROM satuan s WHERE s.nama_satuan = 'Piece' LIMIT 1)
WHERE satuan_Id IS NULL;

ALTER TABLE produk MODIFY satuan_Id INT(11) NOT NULL;

SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'produk'
    AND CONSTRAINT_NAME = 'fk_produk_satuan'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE produk ADD CONSTRAINT fk_produk_satuan FOREIGN KEY (satuan_Id) REFERENCES satuan(satuan_Id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Fractional quantity columns
ALTER TABLE produk MODIFY stok_produk DECIMAL(12,3) NOT NULL;
ALTER TABLE detail_penjualan MODIFY jumlah DECIMAL(12,3) NOT NULL;
ALTER TABLE detail_pembelian MODIFY jumlah DECIMAL(12,3) NOT NULL;

-- 5) Refresh tableproduk view to include unit (used by product list UI)
DROP VIEW IF EXISTS tableproduk;
CREATE VIEW tableproduk AS
SELECT
  p.kode_produk AS kode_produk,
  p.nama_produk AS nama_produk,
  p.harga_beli AS harga_beli,
  p.harga_jual AS harga_jual,
  p.stok_produk AS stok_produk,
  p.satuan_Id AS satuan_Id,
  s.nama_satuan AS nama_satuan,
  s.allow_decimal AS allow_decimal,
  sup.nama_supplier AS nama_supplier,
  k.nama_kategori AS nama_kategori,
  m.nama_merek AS nama_merek
FROM produk p
JOIN supplier sup ON p.supplier_Id = sup.supplier_Id
JOIN kategori k ON p.kategori_Id = k.kategori_Id
JOIN merek m ON p.merek_Id = m.merek_Id
JOIN satuan s ON p.satuan_Id = s.satuan_Id;

COMMIT;

-- 4) Recreate stock triggers (run after COMMIT; DELIMITER required in mysql client / phpMyAdmin)
DROP TRIGGER IF EXISTS kurangiStok;
DROP TRIGGER IF EXISTS restock;

DELIMITER $$

CREATE TRIGGER kurangiStok
BEFORE INSERT ON detail_penjualan
FOR EACH ROW
BEGIN
  UPDATE produk
  SET stok_produk = stok_produk - NEW.jumlah
  WHERE kode_produk = NEW.kode_produk;
END$$

CREATE TRIGGER restock
AFTER INSERT ON detail_pembelian
FOR EACH ROW
BEGIN
  UPDATE produk
  SET stok_produk = stok_produk + NEW.jumlah
  WHERE kode_produk = NEW.kode_produk;
END$$

DELIMITER ;
