-- Migration 008: soft-delete void for penjualan.
-- Run manually against local (MariaDB) and cloud (MySQL 9).
-- Do not use ADD COLUMN IF NOT EXISTS (MariaDB-only).
-- Back up first.

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- Columns
-- ---------------------------------------------------------------------------
ALTER TABLE penjualan
  ADD COLUMN voided TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN voided_at DATETIME NULL,
  ADD COLUMN voided_by INT(11) NULL;

ALTER TABLE penjualan
  ADD CONSTRAINT fk_penjualan_voided_by
    FOREIGN KEY (voided_by) REFERENCES users(user_Id);

COMMIT;

-- ---------------------------------------------------------------------------
-- Views — exclude voided sales (most reports read through these)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW laporan_penjualan AS
SELECT
  p.penjualan_Id AS penjualan_Id,
  p.tanggal_penjualan AS tanggal_penjualan,
  p.Total_pembayaran AS total_pembayaran,
  u.nama_user AS nama_user
FROM penjualan p
JOIN users u ON p.user_Id = u.user_Id
WHERE p.voided = 0;

CREATE OR REPLACE VIEW nota_penjualan AS
SELECT
  detail_penjualan.kode_produk AS kode_produk,
  penjualan.user_Id AS user_Id,
  penjualan.penjualan_Id AS penjualan_Id,
  penjualan.tanggal_penjualan AS tanggal_penjualan,
  penjualan.Total_pembayaran AS Total_pembayaran,
  penjualan.uang_diterima AS uang_diterima,
  penjualan.uang_kembalian AS uang_kembalian,
  detail_penjualan.jumlah AS jumlah,
  detail_penjualan.Subtotal AS Subtotal,
  users.nama_user AS nama_user,
  produk.nama_produk AS nama_produk,
  produk.harga_jual AS harga_jual
FROM penjualan
JOIN detail_penjualan ON penjualan.penjualan_Id = detail_penjualan.penjualan_Id
JOIN users ON penjualan.user_Id = users.user_Id
JOIN produk ON detail_penjualan.kode_produk = produk.kode_produk
WHERE penjualan.voided = 0;

-- ---------------------------------------------------------------------------
-- Procedures — exclude voided sales
-- (DELIMITER required in mysql client / DBeaver "Execute script")
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS JumlahPenjualan;
DROP PROCEDURE IF EXISTS Keuntungan;
DROP PROCEDURE IF EXISTS TotalPendapatan;
DROP PROCEDURE IF EXISTS QuantityPenjualan;
DROP PROCEDURE IF EXISTS TopProduk;
DROP PROCEDURE IF EXISTS TopProduct;

DELIMITER $$

CREATE PROCEDURE JumlahPenjualan(IN tanggalPenjualan DATE, OUT JumlahPenjualan INT)
BEGIN
  SELECT COUNT(*) INTO JumlahPenjualan
  FROM penjualan
  WHERE DATE(tanggal_penjualan) = tanggalPenjualan
    AND voided = 0;
END$$

CREATE PROCEDURE Keuntungan(IN tanggalPenjualan DATE, OUT totalKeuntungan DECIMAL(10,2))
BEGIN
  SELECT SUM((p.harga_jual - p.harga_beli) * dp.jumlah) INTO totalKeuntungan
  FROM detail_penjualan dp
  JOIN produk p ON dp.kode_produk = p.kode_produk
  JOIN penjualan j ON dp.penjualan_Id = j.penjualan_Id
  WHERE DATE(j.tanggal_penjualan) = tanggalPenjualan
    AND j.voided = 0;
END$$

CREATE PROCEDURE TotalPendapatan(IN tanggalPenjualan DATE, OUT totalHargaPenjualan DECIMAL(10,2))
BEGIN
  SELECT SUM(total_Pembayaran) INTO totalHargaPenjualan
  FROM penjualan
  WHERE DATE(tanggal_penjualan) = tanggalPenjualan
    AND voided = 0;
END$$

CREATE PROCEDURE QuantityPenjualan(IN tanggalPenjualan DATE, OUT QuantityPenjualan INT)
BEGIN
  SELECT COUNT(*) INTO QuantityPenjualan
  FROM penjualan
  WHERE DATE(tanggal_penjualan) = tanggalPenjualan
    AND voided = 0;
END$$

CREATE PROCEDURE TopProduk(IN input_count INT)
BEGIN
  SELECT p.*
  FROM produk p
  INNER JOIN (
    SELECT kode_produk
    FROM nota_penjualan
    GROUP BY kode_produk
    ORDER BY SUM(jumlah) DESC
    LIMIT input_count
  ) AS top ON p.kode_produk = top.kode_produk;
END$$

CREATE PROCEDURE TopProduct(IN input_count INT)
BEGIN
  SELECT p.*
  FROM produk p
  INNER JOIN (
    SELECT kode_produk
    FROM nota_penjualan
    GROUP BY kode_produk
    ORDER BY SUM(jumlah) DESC
    LIMIT input_count
  ) AS top ON p.kode_produk = top.kode_produk;
END$$

DELIMITER ;
