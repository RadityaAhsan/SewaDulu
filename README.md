# SewaDulu - Sistem Penyewaan Barang

Aplikasi web penyewaan barang (tenda, kamera, dan sepeda) berbasis **Kotlin** dan **Javalin**, dengan penyimpanan data di **MongoDB Atlas**. Backend menyediakan REST API, sedangkan antarmuka web (HTML, CSS, JavaScript) dilayani langsung oleh server yang sama.

## Fitur

1. **Tambah item**: tenda, kamera, atau sepeda, masing-masing dengan atribut khusus (kapasitas, resolusi, jenis gigi).
2. **Tambah pelanggan**: validasi nama, format email, format nomor HP Indonesia, dan pengecekan email duplikat.
3. **Proses sewa**: memilih item yang tersedia, pelanggan, dan durasi sewa (hari).
4. **Pengembalian**: menghitung total biaya otomatis dan mengembalikan status item menjadi tersedia.
5. **Daftar item dengan filter** status (semua, tersedia, sedang disewa).
6. **Riwayat transaksi** beserta ringkasan statistik (total item, item tersedia, sewa aktif, jumlah pelanggan).

## Teknologi

| Komponen | Versi / Keterangan |
|---|---|
| Bahasa | Kotlin 1.9.24 (JVM toolchain 17) |
| Web framework | Javalin 6.3.0 |
| Database | MongoDB Atlas (`mongodb-driver-sync` 4.11.1), database `sewadulu_db` |
| JSON | Jackson (`jackson-module-kotlin` 2.17.2) |
| Logging | slf4j-simple 2.0.13 |
| Build tool | Gradle (wrapper 9.6.0) |
| Frontend | HTML, CSS, JavaScript (fetch API), ikon Font Awesome via CDN |

## Prasyarat

- JDK 17 atau lebih baru
- Koneksi internet (untuk MongoDB Atlas dan unduhan dependency pertama kali)
- Gradle tidak perlu dipasang manual karena proyek menyertakan Gradle Wrapper (`gradlew`). Proyek juga bisa dibuka langsung di IntelliJ IDEA.

## Konfigurasi Database

Koneksi MongoDB dibaca dari *environment variable* `MONGODB_URI` (lihat `config/MongoConfig.kt`).

```bash
# Linux / macOS
export MONGODB_URI="mongodb+srv://<username>:<password>@<cluster>.mongodb.net/?appName=<nama>"

# Windows (PowerShell)
$env:MONGODB_URI = "mongodb+srv://<username>:<password>@<cluster>.mongodb.net/?appName=<nama>"
```

Koleksi yang dipakai: `items`, `pelanggan`, dan `transaksi`. Koleksi dibuat otomatis oleh MongoDB saat data pertama disimpan.

## Menjalankan Aplikasi

```bash
# Linux / macOS
./gradlew run

# Windows
gradlew.bat run
```

Setelah muncul pesan `SewaDulu berjalan di http://localhost:7070`, buka **http://localhost:7070** di browser.

## Arsitektur

Aplikasi memakai arsitektur berlapis (*layered architecture*). Setiap lapisan hanya berurusan dengan tanggung jawabnya sendiri dan dirakit di `Main.kt` memakai *constructor injection*.

```
Browser (index.html + app.js)
        |  HTTP / JSON
        v
Controller   ->   Service   ->   Repository   ->   MongoDB Atlas
(routing)         (aturan bisnis,  (akses data)
                   validasi)
```

## Struktur Proyek

```
src/main/kotlin/com/sewadulu/
├── Main.kt                         # titik awal: merakit semua lapisan, start server di port 7070
├── config/
│   └── MongoConfig.kt              # koneksi MongoDB (singleton, lazy)
├── model/
│   ├── ItemSewa.kt                 # abstract class ItemSewa + Tenda, Kamera, Sepeda
│   ├── Pelanggan.kt                # data class Pelanggan
│   ├── TransaksiSewa.kt            # data class TransaksiSewa
│   └── Request.kt                  # DTO request (TambahItem, TambahPelanggan, Sewa, Kembali)
├── repository/
│   ├── ItemRepository.kt           # CRUD koleksi "items"
│   ├── PelangganRepository.kt      # CRUD koleksi "pelanggan"
│   └── TransaksiRepository.kt      # CRUD koleksi "transaksi"
├── service/
│   ├── ItemService.kt              # validasi & pembuatan item, filter ketersediaan
│   ├── PelangganService.kt         # validasi pelanggan, cek email duplikat
│   └── TransaksiService.kt         # proses sewa, pengembalian, hitung biaya
├── controller/
│   ├── ItemController.kt           # route /api/items
│   ├── PelangganController.kt      # route /api/pelanggan
│   ├── TransaksiController.kt      # route /api/transaksi, /api/sewa, /api/kembali
│   ├── ErrorHandler.kt             # penangkap exception global -> JSON {"error": "..."}
│   └── ParseBody.kt                # parsing body JSON + konversi error ke exception aplikasi
├── exception/
│   └── SewaDuluException.kt        # sealed class + 5 exception turunan
└── util/
    ├── IdGenerator.kt              # pembuat ID unik (ITM-xxxxxx, CUST-xxxxxx, TRX-xxxxxx)
    └── Validasi.kt                 # validasi email, nomor HP, format satuan

src/main/resources/public/          # frontend statis
├── index.html
├── app.js
└── style.css
```

## Konsep Pemrograman Berorientasi Objek

| Konsep | Penerapan |
|---|---|
| **Abstract class & inheritance** | `ItemSewa` menjadi induk bagi `Tenda`, `Kamera`, dan `Sepeda`. |
| **Polimorfisme** | Fungsi `atributUnik()` dan properti `kategori` di-*override* tiap turunan, sehingga `toMap()` di induk menghasilkan detail yang berbeda untuk tiap jenis item. |
| **Sealed class** | `SewaDuluException` dengan turunan `InputTidakValidException`, `ItemTidakDitemukanException`, `PelangganTidakDitemukanException`, `TransaksiTidakDitemukanException`, dan `ItemTidakTersediaException`. Properti `kode` dipakai sebagai HTTP status code. |
| **Data class** | `Pelanggan`, `TransaksiSewa`, dan seluruh DTO request. |
| **Object (singleton)** | `MongoConfig`, `ErrorHandler`, `IdGenerator`, `Validasi`. |
| **Enkapsulasi & separation of concerns** | Controller, service, dan repository dipisah; repository tidak memuat aturan bisnis, controller tidak memuat logika validasi. |
| **Penanganan exception** | Semua error bisnis ditangkap `ErrorHandler` dan dikirim ke frontend sebagai JSON, sehingga server tidak crash. |
| **Null safety** | Field DTO bertipe nullable agar validasi dilakukan di service, bukan crash saat parsing JSON. |
| **Thread safety** | Method yang mengubah data (`tambah`, `sewa`, `kembalikan`) ditandai `@Synchronized`. |

## Endpoint REST API

| Method | Endpoint | Fungsi | Status sukses |
|---|---|---|---|
| GET | `/api/items` | Daftar item. Filter opsional: `?tersedia=true` atau `?tersedia=false` | 200 |
| POST | `/api/items` | Tambah item baru | 201 |
| GET | `/api/pelanggan` | Daftar pelanggan | 200 |
| POST | `/api/pelanggan` | Tambah pelanggan baru | 201 |
| GET | `/api/transaksi` | Riwayat transaksi (terbaru di atas) | 200 |
| POST | `/api/sewa` | Proses penyewaan | 201 |
| POST | `/api/kembali` | Proses pengembalian | 200 |

**Contoh body request**

```json
// POST /api/items  (kategori: tenda | kamera | sepeda)
{ "kategori": "tenda", "nama": "Tenda Dome 4P", "hargaSewa": 50000, "kapasitas": 4 }
{ "kategori": "kamera", "nama": "Canon EOS 200D", "hargaSewa": 150000, "resolusi": "24" }
{ "kategori": "sepeda", "nama": "Polygon Xtrada", "hargaSewa": 40000, "jenisGigi": "21" }

// POST /api/pelanggan
{ "nama": "Budi Santoso", "email": "budi@email.com", "noHp": "081234567890" }

// POST /api/sewa
{ "itemId": "ITM-3F9A2C", "pelangganId": "CUST-81B2D4", "durasiHari": 3 }

// POST /api/kembali
{ "transaksiId": "TRX-C07E15" }
```

**Format error** (semua error dikembalikan dengan struktur yang sama):

```json
{ "error": "Item 'Tenda Dome 4P' sedang disewa dan tidak tersedia." }
```

| Kode | Penyebab |
|---|---|
| 400 | Input tidak valid (kosong, format salah, email sudah terdaftar, transaksi sudah dikembalikan) |
| 404 | Item, pelanggan, atau transaksi tidak ditemukan |
| 409 | Item sedang disewa dan tidak tersedia |
| 500 | Kesalahan tak terduga pada server |

## Aturan Bisnis dan Validasi

- **Item**: kategori wajib dipilih (tenda, kamera, atau sepeda), nama tidak boleh kosong, harga sewa harus lebih dari 0.
    - Tenda: kapasitas harus lebih dari 0 (disimpan sebagai angka, ditampilkan "N orang").
    - Kamera: resolusi berupa angka; satuan **MP** ditambahkan otomatis (`24`, `24mp`, dan `24 MP` menjadi `24 MP`).
    - Sepeda: jenis gigi berupa angka; satuan **Speed** ditambahkan otomatis (`21` menjadi `21 Speed`).
- **Pelanggan**: nama wajib diisi, email harus berformat valid dan unik (tidak membedakan huruf besar/kecil), nomor HP harus berformat Indonesia (`08xx`, `628xx`, atau `+628xx`) dan spasi/tanda hubung dibersihkan otomatis.
- **Sewa**: item, pelanggan, dan durasi (minimal 1 hari) wajib diisi; item harus berstatus tersedia. Saat sewa berhasil, status item menjadi tidak tersedia. Harga per hari disimpan pada transaksi sehingga perubahan harga di kemudian hari tidak memengaruhi transaksi yang sudah berjalan.
- **Pengembalian**: transaksi yang sudah dikembalikan tidak dapat diproses ulang. Setelah pengembalian, status item kembali menjadi tersedia.
- **Perhitungan biaya**: `total biaya = harga per hari x jumlah hari aktual` (dihitung dari tanggal sewa sampai tanggal kembali, minimal 1 hari).

## Penyimpanan Data

Data disimpan secara permanen di MongoDB Atlas (database `sewadulu_db`), sehingga **tidak hilang** saat server dimatikan. Repository juga dibuat toleran terhadap data lama (misalnya field kosong atau bertipe berbeda) agar aplikasi tidak error ketika membaca dokumen yang formatnya lebih lama.

## Perubahan dari Versi Sebelumnya

| Aspek | Versi lama | Versi sekarang |
|---|---|---|
| Penyimpanan data | Memori (`MutableMap` & `MutableList`), hilang saat server mati | MongoDB Atlas, permanen |
| Arsitektur | `SewaService` dan `SewaController` tunggal | Dipisah menjadi controller, service, dan repository per entitas (item, pelanggan, transaksi) |
| Exception | 5 turunan `SewaDuluException` | Tetap, ditambah `ErrorHandler` global dan `ParseBody` untuk JSON yang rusak |
| Utilitas | Validasi di dalam service | `Validasi` dan `IdGenerator` dipisah ke package `util` |
| Validasi | Dasar | Format email, nomor HP Indonesia, email unik, satuan otomatis (MP / Speed) |
| Frontend | Satu halaman sederhana | Dashboard statistik, filter item, riwayat transaksi, dan notifikasi (toast) |
| Perhitungan biaya | Berdasarkan durasi | Berdasarkan hari aktual pemakaian (minimal 1 hari) |
| Build | Gradle 8+ | Gradle Wrapper 9.6.0, Kotlin 1.9.24, JDK 17 |

## Catatan Pengembangan

- Dependency `sqlite-jdbc` dan `kotlinx-serialization-json` masih tercantum di `build.gradle.kts` tetapi tidak dipakai oleh kode saat ini; dapat dihapus pada pembersihan berikutnya.
- Kredensial database sebaiknya hanya diberikan lewat environment variable `MONGODB_URI` dan tidak ditulis langsung di kode maupun di repository.
