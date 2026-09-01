-- Scale/weighed products flag (PLU / Rongta barcode).
-- Run on local (localhost:3307/pointofsale) and Railway cloud MySQL.

ALTER TABLE produk ADD COLUMN is_scale TINYINT(1) NOT NULL DEFAULT 0;

UPDATE produk SET is_scale = 1
WHERE kategori_Id = (
  SELECT kategori_Id FROM kategori WHERE nama_kategori = 'Vegetables' LIMIT 1
);

-- pos-api (Railway): in GET /api/sync/changes products query, include is_scale
-- in the SELECT and JSON payload (e.g. isScale / is_scale) so desktop pull applies it.
