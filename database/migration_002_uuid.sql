-- Migration 002: UUID + updated_at for cloud sync prep
-- Run manually against the existing pointofsale database.
-- Do NOT replace integer PKs / FKs. Do not add PKs to detail tables.
-- Back up first.

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- Helper pattern per table:
--   1) add uuid NULL
--   2) backfill UUID()
--   3) NOT NULL + UNIQUE
--   4) add updated_at
-- ---------------------------------------------------------------------------

-- produk
ALTER TABLE produk ADD COLUMN uuid CHAR(36) NULL;
UPDATE produk SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE produk MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE produk ADD UNIQUE KEY uk_produk_uuid (uuid);
ALTER TABLE produk
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- penjualan
ALTER TABLE penjualan ADD COLUMN uuid CHAR(36) NULL;
UPDATE penjualan SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE penjualan MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE penjualan ADD UNIQUE KEY uk_penjualan_uuid (uuid);
ALTER TABLE penjualan
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- detail_penjualan (no PK — do not add one)
ALTER TABLE detail_penjualan ADD COLUMN uuid CHAR(36) NULL;
UPDATE detail_penjualan SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE detail_penjualan MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE detail_penjualan ADD UNIQUE KEY uk_detail_penjualan_uuid (uuid);
ALTER TABLE detail_penjualan
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- pembelian
ALTER TABLE pembelian ADD COLUMN uuid CHAR(36) NULL;
UPDATE pembelian SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE pembelian MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE pembelian ADD UNIQUE KEY uk_pembelian_uuid (uuid);
ALTER TABLE pembelian
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- detail_pembelian (no PK — do not add one)
ALTER TABLE detail_pembelian ADD COLUMN uuid CHAR(36) NULL;
UPDATE detail_pembelian SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE detail_pembelian MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE detail_pembelian ADD UNIQUE KEY uk_detail_pembelian_uuid (uuid);
ALTER TABLE detail_pembelian
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- kategori
ALTER TABLE kategori ADD COLUMN uuid CHAR(36) NULL;
UPDATE kategori SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE kategori MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE kategori ADD UNIQUE KEY uk_kategori_uuid (uuid);
ALTER TABLE kategori
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- merek
ALTER TABLE merek ADD COLUMN uuid CHAR(36) NULL;
UPDATE merek SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE merek MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE merek ADD UNIQUE KEY uk_merek_uuid (uuid);
ALTER TABLE merek
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- supplier
ALTER TABLE supplier ADD COLUMN uuid CHAR(36) NULL;
UPDATE supplier SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE supplier MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE supplier ADD UNIQUE KEY uk_supplier_uuid (uuid);
ALTER TABLE supplier
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- satuan
ALTER TABLE satuan ADD COLUMN uuid CHAR(36) NULL;
UPDATE satuan SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE satuan MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE satuan ADD UNIQUE KEY uk_satuan_uuid (uuid);
ALTER TABLE satuan
  ADD COLUMN updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

COMMIT;
