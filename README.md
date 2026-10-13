# SewaDulu - Sistem Penyewaan Barang (Kotlin + Javalin)

## Prasyarat
- JDK 17+
- Gradle 8+ (atau buka folder ini di IntelliJ IDEA, otomatis di-import)

## Menjalankan
    gradle run
Buka http://localhost:7070

## Struktur
- model/      : ItemSewa (abstract), Tenda, Kamera, Sepeda, Pelanggan, TransaksiSewa, DTO request
- exception/  : SewaDuluException (sealed class) + 5 turunan
- service/    : SewaService (business logic, validasi, MutableMap & MutableList)
- controller/ : SewaController (routing API + exception handler)
- resources/public/index.html : frontend (fetch API)

Data disimpan di memori, hilang saat server dimatikan.
