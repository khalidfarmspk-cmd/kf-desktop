-- Migration 015: record the clock time of a sale.
-- penjualan.tanggal_penjualan is a DATE, so HOUR() on it is always 0 and the
-- dashboard's sales-by-hour chart could never plot anything.
-- Existing rows are backfilled from updated_at, which equals creation time for
-- any sale that was never edited afterwards. Approximate, but better than null.
-- Run manually against local (MariaDB) and cloud (MySQL 9).
-- Back up first.
-- Do NOT run from the app.

ALTER TABLE penjualan ADD COLUMN waktu_penjualan DATETIME NULL;

UPDATE penjualan SET waktu_penjualan = updated_at WHERE waktu_penjualan IS NULL;

CREATE OR REPLACE VIEW laporan_penjualan AS
SELECT p.penjualan_Id      AS penjualan_Id,
       p.tanggal_penjualan AS tanggal_penjualan,
       p.waktu_penjualan   AS waktu_penjualan,
       p.Total_pembayaran  AS total_pembayaran,
       u.nama_user         AS nama_user
FROM penjualan p
JOIN users u ON p.user_Id = u.user_Id
WHERE p.voided = 0;
