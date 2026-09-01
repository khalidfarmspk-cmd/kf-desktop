# Point of Sale Desktop — Complete Application Summary

This document describes **everything** the application currently has: modules, business logic, database, printing, roles, and how major flows work.

| Item | Detail |
|------|--------|
| Project folder | `PointOfSale-Desktop` |
| Type | Java Swing desktop Point of Sale (POS) |
| UI language | English (editorial / admin style) |
| Currency display | **Rs** (rupees) |
| Database | MySQL / MariaDB — schema `pointofsale` |
| Default connection | `localhost:3307`, user `root`, empty password (`config/Koneksi.java`) |
| Entry class | `view.Form_Login_old` |
| Build | NetBeans Ant project under `app/` (Java 8 target) |

---

## 1. What the application is for

A small-shop / retail POS for:

- Selling products at a register (cart, discount, customer, payment, receipt)
- Managing products, categories, customers, suppliers, and staff accounts
- Restocking inventory from suppliers
- Tracking expenses
- Owner dashboards and profit reports
- Thermal receipts and shelf barcode labels
- Excel import / export of the product catalog

It is **single-store desktop software** (not a web app). UUIDs were added so records can later sync to a cloud backend without changing integer primary keys.

---

## 2. Technology stack

| Layer | Technology |
|-------|------------|
| Language | Java 8 |
| UI | Swing + **FlatLaf** light theme |
| Database | MySQL Connector/J 5.x |
| Auth | **jBCrypt** (with one-time plaintext → hash upgrade on login) |
| Reports | JasperReports 5.6 (`produk.jrxml`, `nota_penjualan.jrxml`) |
| Excel | Apache POI 5.2.5 (`.xlsx`) |
| Barcode / QR | barcode lib, ZXing, qrgen |
| Calendar | jcalendar |
| Printing | ESC/POS (receipts), ZPL (labels), Windows RAW print helper |

Third-party JARs live in `library/`.

---

## 3. How to run

1. Start MySQL (e.g. XAMPP) listening on **port 3307**.
2. Create / import database `pointofsale` from `database/PointOfSale.sql`.
3. Apply migrations in order (see §8).
4. Open `app/` in NetBeans **or** compile classes into `app/build/classes` with `library/*.jar` on the classpath.
5. Run: `view.Form_Login_old`.

**Default accounts** (after seed / README):

| Role | Username | Password |
|------|----------|----------|
| Owner | `admin` | `admin` |
| Employee | `karyawan` | `karyawan` |

(Live DBs may use different users, e.g. `admin` / `usama`, after staff edits.)

---

## 4. Login, session, and roles

### Flow

```
Form_Login_old
  → DAO_Login.prosesLogin()
  → verifies username + password + status AKTIF
  → fills Main.user static session
  → opens Menu_Utama(nama, level) maximized
```

### Roles

| DB `level_user` | UI name | Access |
|-----------------|---------|--------|
| `PEMILIK` (also accepted as `Owner`) | Owner | Full menu |
| `KARYAWAN` (also accepted as `Employee`) | Employee | Sell, Customers, Today's sales |

Inactive users (`status_user` ≠ active) cannot log in.

### Session holders

- `Main.user` — id, name, username, password, role (primary)
- `Main.UserSession` — alternate statics used by some user-profile paths

Logout returns to `Form_Login_old`. Profile opens `Form_Akun` (change password).

---

## 5. Main shell and navigation (`Menu_Utama`)

Sidebar “POINT OF SALE / LAHORE” style chrome with numbered nav items. Content host is a `CardLayout` panel.

### Owner menu

| Section | Pages |
|---------|--------|
| Overview | Dashboard |
| Master | Products, Categories, Customers, Suppliers, Users |
| Transactions | Sell, Restock, Expenses, Reports |
| Settings | Invoice |

Owner home page: **Dashboard**.

### Employee menu

| Pages |
|-------|
| Sell (home) |
| Customers |
| Today's sales |

Employees do **not** get Dashboard, Products, Restock, Expenses, full Reports, Users, or Invoice settings.

Header chrome shows **DATE**, **SIGNED IN** (`username · Owner/Employee`), and **Profile**.

---

## 6. Module catalog (every screen)

### `Main/` — operations & chrome

| Class | Purpose |
|-------|---------|
| `Menu_Utama` | Main window, role-based sidebar, page switching |
| `MenuItem` | Sidebar row (index + label + click → swap content) |
| `Form_Penjualan` | POS sell screen |
| `Form_Pembelian` | Restock / purchase screen |
| `Form_DasbordPemilik` | Owner dashboard KPIs & charts |
| `Form_DasbordKaryawan` | Employee dashboard (exists; employee nav currently lands on Sell) |
| `Form_ReportPemilik` | Owner reports, void sale, CSV export, reprint |
| `Form_ReportKaryawan` | Employee today-only sales + reprint |
| `Form_Pengeluaran` | Expenses CRUD |
| `Form_Pengaturan` | Shop / invoice / receipt settings |
| `Form_Akun` | Logged-in user profile & password change |
| `ReceiptPrinter` | ESC/POS thermal receipt builder |
| `RawPrinter` | Windows RAW bytes to a named printer (ZPL-friendly) |
| `UITheme` | Colors, fonts, FlatLaf setup, currency |
| `PageUI` | Shared page headers, buttons, tables |
| `QuantityUtil` | Decimal qty parse / money line totals |
| `user` / `UserSession` | Session statics |

### `Master/` — master data

| Class | Purpose |
|-------|---------|
| `Form_Barang` | Products CRUD, barcode preview, labels, Excel I/O, Jasper product list |
| `ProductExcel` | Product `.xlsx` import & export |
| `Form_Kategori` | Categories (+ shelf/rack number) |
| `Form_Pelanggan` | Customers (no login) |
| `Form_Suplier` | Suppliers |
| `Form_User` | Staff users (role, active/suspended, BCrypt) |

**Brand (`merek`) UI was removed.** Products still store `merek_Id` pointing at a hidden default brand named **General**.

### `view/` / `config/` / `dao/` / `service/` / `model/`

| Class | Purpose |
|-------|---------|
| `Form_Login_old` | Login window (app entry) |
| `Koneksi` | JDBC singleton |
| `Ids` | `UUID.randomUUID()` helper |
| `Settings` | Read/write `pengaturan` key/value |
| `DAO_Login` | Auth implementation |
| `Encrypt` | Legacy MD5 helper (not used by current login path) |
| `Service_Login` | Login interface |
| `Model_login` | Username/password DTO |

### `report/`

| File | Purpose |
|------|---------|
| `produk.jrxml` / `.jasper` | Product list report |
| `nota_penjualan.jrxml` / `.jasper` | Legacy sale nota (live sales print via ESC/POS) |

---

## 7. Business logic — how each flow works

### 7.1 Sell (`Form_Penjualan`)

1. Load products (category chips, search/barcode, stock, sell price, unit, whether decimals allowed).
2. Add lines to cart; quantity validated; **cannot exceed stock**.
3. Optional **customer** (typeahead from `pelanggan`; can open Customers form).
4. Optional **bill discount** (rupees), capped by setting `max_discount_percent` (default 10% of subtotal).
5. Choose **payment method** from `metode_bayar` where `aktif = 1`.
   - **Cash** → amount received + change fields.
   - Non-cash → treated as paid in full.
   - **Cash on Delivery / Card on Delivery** → require **delivery man** name (`nama_kurir`).
6. On confirm (transaction):
   - Insert `penjualan` (totals, discount, customer, method, courier, cashier, UUID).
   - Insert each `detail_penjualan` line (qty, subtotal, UUID).
   - Trigger `kurangiStok` decreases `produk.stok_produk`.
7. Call `ReceiptPrinter.printReceipt(saleId)`.

### 7.2 Restock (`Form_Pembelian`)

1. Pick products + quantities (and supplier context as designed in UI).
2. Insert `pembelian` + `detail_pembelian` with UUIDs.
3. Trigger `restock` **adds** stock on each detail insert.

### 7.3 Products (`Form_Barang` + `ProductExcel`)

- CRUD on `produk` (code, name, buy, sell, stock, category, unit, supplier).
- Brand combo is hidden; save uses `ensureDefaultMerekId()` → **General**.
- Units from `satuan` (`allow_decimal` drives fractional stock/qty).
- Barcode image preview / generate under app image paths.
- **Print list** → Jasper `produk`.
- **Print Label** → ZPL to a Windows printer whose name contains **`LP 2824`** (2×1" style: name, barcode, Rs price/unit).
- **Import Excel** / **Export Excel** → `.xlsx` via Apache POI.

Excel columns:

```text
CODE | PRODUCT | CATEGORY | BUY | SELL | STOCK | UNIT | SUPPLIER
```

Import rules:

- `CODE` + `PRODUCT` required.
- Matching code → update; new code → insert.
- Missing category / unit / supplier names are created as needed.
- Brand forced to General.

### 7.4 Categories, customers, suppliers, users, expenses

| Module | Behavior |
|--------|----------|
| Categories | Name + rack/shelf number |
| Customers | Name, phone, address — used on sales only (no login) |
| Suppliers | Name, address, phone — linked to products |
| Users | Owner/Employee, Active/Suspended; passwords BCrypt; cannot delete yourself |
| Expenses | Date, category (Rent, Utilities, Salaries, …), note, amount; reduces net profit |

### 7.5 Owner dashboard (`Form_DasbordPemilik`)

Typical KPIs for **today**:

- Revenue (sum of sale totals)
- Sale count
- **Net profit** (see formula below)
- Product / stock attention (low stock)
- Top products
- Sales-by-hour style chart

### 7.6 Reports

**Owner (`Form_ReportPemilik`)**

- Date range / presets
- KPIs: sales count, revenue, **net profit**
- Charts / top products / transaction list + line detail
- **Void sale** (owner only): restore stock from lines, then delete `detail_penjualan` + `penjualan`
- Export CSV
- Reprint receipt

**Employee (`Form_ReportKaryawan`)**

- Today's sales focus
- Search / line detail / reprint
- **No void**

### 7.7 Profit formula (dashboard & owner reports)

The UI **does not** rely on the old stored procedure `Keuntungan` alone (that was list-price margin only).

Current **net profit**:

```text
Σ (line Subtotal − qty × buy_price)
  − Σ bill discounts (penjualan.diskon)
  − Σ expenses (pengeluaran.jumlah)
```

for the selected day or date range.

### 7.8 Invoice / receipt settings (`Form_Pengaturan`)

Key/value rows in `pengaturan`, including:

| Key | Meaning |
|-----|---------|
| `shop_name` | Header name |
| `shop_address` | Address |
| `shop_phone` | Phone |
| `receipt_footer` | Footer lines |
| `max_discount_percent` | Cap for sell-screen discount |
| `logo_path` | Shop logo file |
| `show_logo` | Toggle logo on receipt |
| `show_invoice_barcode` | Toggle invoice barcode |
| `show_cashier` / `show_customer` / `show_payment_method` | Receipt sections |

Supports on-screen preview and test print.

### 7.9 Auth details

- Lookup by username.
- If hash looks like BCrypt (`$2…`) → `BCrypt.checkpw`.
- Else if plaintext matches → accept and **rewrite** password as BCrypt.
- New / reset passwords: `BCrypt.gensalt(10)`.
- `Encrypt` MD5 is legacy only.

---

## 8. Database

### Connection

```text
jdbc:mysql://localhost:3307/pointofsale
user: root
password: (empty)
```

Defined in `app/src/config/Koneksi.java`.

### Install order

1. `database/PointOfSale.sql` — base schema, views, procedures, triggers, seed users  
2. `migration_satuan_decimal_qty.sql` — units + fractional qty  
3. `migration_002_uuid.sql` — `uuid` + `updated_at` on sync tables  
4. `migration_003_auth.sql` — longer password field + unique username  
5. `migration_004_billing.sql` — customers, payment methods, discount, expenses, settings  
6. `migration_005_invoice.sql` — logo / barcode settings; text values  
7. `migration_006_payment_delivery.sql` — COD payment methods  
8. `migration_007_delivery_man.sql` — `nama_kurir` on sales  

Also keep a dump such as `pos_backup.sql` for recovery.

### Base tables (live shape)

| Table | Purpose | Important columns |
|-------|---------|-------------------|
| `users` | Staff login | `user_Id`, name, username, password (BCrypt), `level_user`, `status_user` |
| `produk` | Products | `kode_produk` PK, name, buy/sell, `stok_produk` DECIMAL, FKs: supplier, kategori, merek, satuan, `uuid` |
| `kategori` | Categories | name, `no_rak`, `uuid` |
| `merek` | Brands (UI hidden) | name (default **General**), `uuid` |
| `supplier` | Suppliers | name, address, phone, `uuid` |
| `satuan` | Units | name, `allow_decimal`, `uuid` |
| `penjualan` | Sale header | date, totals, cash/change, cashier, profit field, customer, method, courier, `subtotal_kotor`, `diskon`, `uuid` |
| `detail_penjualan` | Sale lines | qty DECIMAL, subtotal, product, sale id, `uuid` |
| `pembelian` | Purchase header | date, user, `uuid` |
| `detail_pembelian` | Purchase lines | qty DECIMAL, product, purchase id, `uuid` |
| `pelanggan` | Customers | name, phone, address, `uuid` |
| `metode_bayar` | Payment methods | name, `aktif`, `uuid` |
| `pengeluaran` | Expenses | date, category, note, amount, user, `uuid` |
| `pengaturan` | Settings KV | `setting_key`, `setting_value` |

### Views

| View | Purpose |
|------|---------|
| `tableproduk` | Product list joined with supplier, category, brand, unit |
| `tableusers` | Users without exposing password |
| `laporan_penjualan` | Sale summary for reporting |
| `nota_penjualan` | Line-level join for nota / legacy Jasper |

### Triggers

| Trigger | On | Effect |
|---------|-----|--------|
| `kurangiStok` | `detail_penjualan` INSERT | `stok_produk -= jumlah` |
| `restock` | `detail_pembelian` INSERT | `stok_produk += jumlah` |

### Stored procedures (legacy helpers still in DB)

| Procedure | Purpose |
|-----------|---------|
| `JumlahPenjualan` / `QuantityPenjualan` | Count sales for a date |
| `TotalPendapatan` | Sum revenue for a date |
| `Keuntungan` | Old margin-only profit (UI uses richer formula) |
| `ProdukKurangDari` / `ProductKurangDari` | Low-stock list |
| `TopProduk` / `TopProduct` | Top sellers |

### Seeded payment methods

Cash, Card, Bank Transfer, Easypaisa, JazzCash, Cash on Delivery, Card on Delivery.

### Seeded units

Piece, Dozen, Packet, Box, Bottle, Bag, Kg, Gram, Litre, ml, Metre  
(`allow_decimal = 1` for weight/volume/length).

---

## 9. Printing

| Kind | Class / asset | Target device (by name) | Notes |
|------|---------------|-------------------------|-------|
| Sale receipt | `ReceiptPrinter` | Windows printer **`Black Copper 80`** | ESC/POS, 80mm / 48 cols; logo, shop header, lines, discount, totals, optional barcode |
| Shelf label | `Form_Barang` + `RawPrinter` | Name contains **`LP 2824`** | ZPL; product name, CODE128, Rs price/unit |
| Product list | Jasper `produk.jrxml` | Viewer / print dialog | A4-style catalog |
| Legacy nota | Jasper `nota_penjualan.jrxml` | Viewer | Present; live path prefers ESC/POS |

Receipt content respects `pengaturan` toggles (logo, cashier, customer, payment method, invoice barcode). Transaction number line was removed from the modern receipt layout; invoice # remains.

---

## 10. Excel product exchange

| Action | UI | File | Behavior |
|--------|-----|------|----------|
| Export | Products → **Export Excel** | `.xlsx` | All products with unit & supplier names |
| Import | Products → **Import Excel** | `.xlsx` / `.xls` | Upsert by CODE; create missing category/unit/supplier |

Implemented in `Master/ProductExcel.java`, buttons on `Form_Barang`.

---

## 11. Design decisions (current product)

1. **English UI** over Indonesian DB column names.
2. **Editorial light theme** — white pages, hairline grid, coral/red accent, FlatLaf (`UITheme` / `PageUI`).
3. **Brand module removed** — FK kept via silent **General** merek.
4. **UUID + `updated_at`** on operational tables for future sync; integer PKs unchanged.
5. **Bill-level discount only** (not per line), capped by settings %.
6. **Fractional quantities** when unit allows decimals (Kg, Litre, …).
7. **Customers ≠ users** — shoppers never log in.
8. **Delivery payments** require courier name on the sale.
9. **Void** restores stock then hard-deletes the sale (owner reports only).
10. **Currency symbol Rs** everywhere in UI/receipts.

---

## 12. Project folder map

```text
PointOfSale-Desktop/
├── app/
│   ├── src/
│   │   ├── Main/          # Sell, restock, dashboards, reports, chrome, printers
│   │   ├── Master/        # Products, categories, customers, suppliers, users, Excel
│   │   ├── view/          # Login
│   │   ├── config/        # Koneksi, Ids, Settings
│   │   ├── dao/ service/ model/
│   │   ├── report/        # Jasper templates
│   │   └── img/           # Icons / barcode images
│   ├── build/classes/     # Compiled output
│   ├── data/              # Runtime assets (e.g. shop logo)
│   └── nbproject/         # NetBeans project
├── database/              # Base SQL + numbered migrations
├── library/               # Third-party JARs
├── documentation/         # Screenshots
├── README.md              # Original setup notes
└── APPLICATION_SUMMARY.md # This file
```

---

## 13. Library JARs (grouped)

| Group | JARs |
|-------|------|
| UI | `flatlaf-3.5.4.jar`, `AbsoluteLayout.jar`, `jcalendar-1.4.jar` |
| DB | `mysql-connector-java-5.1.49.jar`, `rs2xml.jar` |
| Auth | `jbcrypt-0.4.jar` |
| Jasper | `jasperreports-5.6.0.jar`, `groovy-all-1.7.5.jar`, `itext-2.1.7.jar`, commons-* digester/beanutils/logging/collections |
| Excel (POI) | `poi-5.2.5.jar`, `poi-ooxml-5.2.5.jar`, `poi-ooxml-lite-5.2.5.jar`, `xmlbeans-5.1.1.jar`, `commons-compress`, `commons-io`, `commons-collections4`, `commons-math3`, `curvesapi`, `sparsebitset`, `log4j-api` |
| Barcode / QR | `barcode.jar`, `zxing-core-1.7.jar`, `zxing-j2se-1.7.jar`, `qrgen-1.0.jar` |

---

## 14. End-to-end picture

```text
                    ┌─────────────┐
                    │ Form_Login  │
                    └──────┬──────┘
                           │ BCrypt + role
           ┌───────────────┴───────────────┐
           ▼                               ▼
     Owner Menu_Utama                Employee Menu_Utama
           │                               │
   Dashboard / Master /              Sell / Customers /
   Sell / Restock /                  Today's sales
   Expenses / Reports /
   Invoice settings
           │
           ├─ Form_Penjualan ──► penjualan + detail_penjualan
           │                      trigger −stock ──► ReceiptPrinter
           ├─ Form_Pembelian ──► pembelian + detail_pembelian
           │                      trigger +stock
           ├─ Form_Barang ─────► produk (+ Excel, ZPL label, Jasper)
           ├─ Form_Pengeluaran ► pengeluaran (cuts profit)
           └─ Form_Report* ────► KPIs, CSV, void (owner), reprint
```

---

## 15. Known operational notes

- Printer names are **hard-coded** (`Black Copper 80`, `LP 2824` substring). Rename Windows printers or change source if hardware differs.
- JDBC URL port **3307** must match your MySQL instance.
- Some Indonesian identifiers remain in SQL/code (`penjualan`, `produk`, `PEMILIK`) while the UI shows English.
- Old procedure `Keuntungan` still exists but dashboard/reports use the **net** formula including discounts and expenses.
- POI jars must be on the runtime classpath for Excel buttons to work.

---

*Generated as a living project summary for PointOfSale-Desktop. Update this file when modules, schema, or formulas change.*
