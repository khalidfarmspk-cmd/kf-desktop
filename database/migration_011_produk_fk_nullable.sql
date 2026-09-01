-- Allow missing FK UUID lookups during cloud pull (null-safe INSERT/UPDATE).
-- supplier_Id / kategori_Id / merek_Id may already be NULL from earlier manual ALTERs.
ALTER TABLE produk MODIFY COLUMN satuan_Id INT NULL;
ALTER TABLE produk MODIFY COLUMN supplier_Id INT NULL;
ALTER TABLE produk MODIFY COLUMN kategori_Id INT NULL;
ALTER TABLE produk MODIFY COLUMN merek_Id INT NULL;
