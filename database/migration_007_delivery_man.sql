-- Migration 007: delivery man name on delivery sales.
-- Used when payment method is Cash on Delivery / Card on Delivery.

START TRANSACTION;

ALTER TABLE penjualan
  ADD COLUMN nama_kurir VARCHAR(60) NULL AFTER metode_Id;

COMMIT;
