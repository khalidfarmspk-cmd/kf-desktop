-- Migration 006: additional payment methods for delivery sales.
-- Run manually against pointofsale (app also applies pending migrations when asked).

START TRANSACTION;

INSERT INTO metode_bayar (nama_metode, aktif, uuid)
SELECT 'Cash on Delivery', 1, UUID()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM metode_bayar WHERE nama_metode = 'Cash on Delivery'
);

INSERT INTO metode_bayar (nama_metode, aktif, uuid)
SELECT 'Card on Delivery', 1, UUID()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM metode_bayar WHERE nama_metode = 'Card on Delivery'
);

COMMIT;
