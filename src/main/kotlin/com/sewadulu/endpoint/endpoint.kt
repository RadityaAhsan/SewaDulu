package com.sewadulu.endpoint

import com.sewadulu.DatabaseService
import com.sewadulu.exception.*
import com.sewadulu.model.KembaliRequest
import com.sewadulu.model.SewaRequest
import com.sewadulu.model.TambahItemRequest
import com.sewadulu.model.TambahPelangganRequest
import io.javalin.Javalin
import io.javalin.http.Context
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

private val lock = Any()

private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
// Format nomor HP Indonesia: 08xx / 628xx / +628xx
private val hpRegex = Regex("^(\\+62|62|0)8[0-9]{8,11}$")

private fun newId(prefix: String) =
    prefix + "-" + UUID.randomUUID().toString().substring(0, 6).uppercase()

/** JSON rusak / tipe data salah diubah menjadi custom exception. */
private inline fun <reified T> Context.parseBody(): T =
    try {
        this.bodyAsClass(T::class.java)
    } catch (e: Exception) {
        throw InputTidakValidException("Format data tidak valid. Periksa kembali isian Anda.")
    }

/** Ubah baris DB menjadi format yang dibaca frontend (detail berupa map). */
private fun itemKeFrontend(item: Map<String, Any>): Map<String, Any> {
    val kunciDetail = when (item["kategori"].toString().lowercase()) {
        "tenda" -> "kapasitas"
        "kamera" -> "resolusi"
        "sepeda" -> "jenisGigi"
        else -> "info"
    }
    return mapOf(
        "id" to item["id"]!!,
        "kategori" to item["kategori"]!!,
        "nama" to item["nama"]!!,
        "hargaSewa" to item["hargaSewa"]!!,
        "statusTersedia" to item["statusTersedia"]!!,
        "detail" to mapOf(kunciDetail to (item["detail"] ?: ""))
    )
}

fun endpointdb(app: Javalin) {

    // Penangkap exception: aplikasi tidak crash, frontend menerima JSON {error: "..."}
    app.exception(SewaDuluException::class.java) { e, ctx ->
        ctx.status(e.kode).json(mapOf("error" to e.message))
    }
    app.exception(Exception::class.java) { e, ctx ->
        e.printStackTrace()
        ctx.status(500).json(mapOf("error" to "Terjadi kesalahan pada server. Silakan coba lagi."))
    }

    // ==========================================
    // 1. ITEMS (/api/items)
    // ==========================================
    app.get("/api/items") { ctx ->
        val semua = DatabaseService.getAllItems()
        val hasil = when (ctx.queryParam("tersedia")?.trim()?.lowercase()) {
            null, "", "semua" -> semua
            "true" -> semua.filter { it["statusTersedia"] == true }
            "false" -> semua.filter { it["statusTersedia"] == false }
            else -> throw InputTidakValidException("Filter tidak valid. Gunakan: true, false, atau kosong.")
        }
        ctx.json(hasil.map { itemKeFrontend(it) })
    }

    app.post("/api/items") { ctx ->
        val req = ctx.parseBody<TambahItemRequest>()

        val kategori = req.kategori?.trim()?.lowercase()
        if (kategori.isNullOrEmpty()) throw InputTidakValidException("Kategori wajib dipilih.")

        val nama = req.nama?.trim()
        if (nama.isNullOrEmpty()) throw InputTidakValidException("Nama item tidak boleh kosong.")

        val harga = req.hargaSewa
        if (harga == null || harga <= 0) throw InputTidakValidException("Harga sewa harus diisi dan lebih dari 0.")

        val detail: String = when (kategori) {
            "tenda" -> {
                val kapasitas = req.kapasitas
                if (kapasitas == null || kapasitas <= 0)
                    throw InputTidakValidException("Kapasitas tenda harus diisi dan lebih dari 0.")
                "$kapasitas orang"
            }
            "kamera" -> {
                val resolusi = req.resolusi?.trim()
                if (resolusi.isNullOrEmpty())
                    throw InputTidakValidException("Resolusi kamera tidak boleh kosong.")
                resolusi
            }
            "sepeda" -> {
                val gigi = req.jenisGigi?.trim()
                if (gigi.isNullOrEmpty())
                    throw InputTidakValidException("Jenis gigi sepeda tidak boleh kosong.")
                gigi
            }
            else -> throw InputTidakValidException("Kategori '$kategori' tidak dikenal (pilih: tenda, kamera, sepeda).")
        }

        val id = newId("ITM")
        synchronized(lock) {
            DatabaseService.insertItem(
                id = id,
                kategori = kategori.replaceFirstChar { it.uppercase() },
                nama = nama,
                hargaSewa = harga.toInt(),
                detail = detail
            )
        }

        ctx.status(201).json(mapOf("id" to id, "nama" to nama, "message" to "Item berhasil ditambahkan!"))
    }

    // ==========================================
    // 2. PELANGGAN (/api/pelanggan)
    // ==========================================
    app.get("/api/pelanggan") { ctx ->
        ctx.json(DatabaseService.getAllPelanggan())
    }

    app.post("/api/pelanggan") { ctx ->
        val req = ctx.parseBody<TambahPelangganRequest>()

        val nama = req.nama?.trim()
        if (nama.isNullOrEmpty()) throw InputTidakValidException("Nama pelanggan tidak boleh kosong.")

        val email = req.email?.trim()
        if (email.isNullOrEmpty() || !emailRegex.matches(email))
            throw InputTidakValidException("Format email tidak valid (contoh: nama@email.com).")

        val noHp = req.noHp?.trim()?.replace(" ", "")?.replace("-", "")
        if (noHp.isNullOrEmpty() || !hpRegex.matches(noHp))
            throw InputTidakValidException("Format nomor HP tidak valid (contoh: 081234567890).")

        val id = newId("CUST")
        synchronized(lock) {
            if (DatabaseService.emailSudahTerdaftar(email))
                throw InputTidakValidException("Email '$email' sudah terdaftar.")
            DatabaseService.insertPelanggan(id = id, nama = nama, email = email, noHp = noHp)
        }

        ctx.status(201).json(mapOf("id" to id, "nama" to nama, "message" to "Pelanggan berhasil ditambahkan!"))
    }

    // ==========================================
    // 3. TRANSAKSI, SEWA, KEMBALI
    // ==========================================
    app.get("/api/transaksi") { ctx ->
        ctx.json(DatabaseService.getAllTransaksi())
    }

    app.post("/api/sewa") { ctx ->
        val req = ctx.parseBody<SewaRequest>()

        val itemId = req.itemId?.trim()
        if (itemId.isNullOrEmpty()) throw InputTidakValidException("Item wajib dipilih.")
        val pelangganId = req.pelangganId?.trim()
        if (pelangganId.isNullOrEmpty()) throw InputTidakValidException("Pelanggan wajib dipilih.")
        val durasi = req.durasiHari
        if (durasi == null || durasi <= 0) throw InputTidakValidException("Durasi sewa minimal 1 hari.")

        val hasil = synchronized(lock) {
            val item = DatabaseService.getItemById(itemId) ?: throw ItemTidakDitemukanException(itemId)
            val pelanggan = DatabaseService.getPelangganById(pelangganId)
                ?: throw PelangganTidakDitemukanException(pelangganId)

            val itemNama = item["nama"].toString()
            if (item["statusTersedia"] != true) throw ItemTidakTersediaException(itemNama)

            val hargaPerHari = (item["hargaSewa"] as Number).toInt()
            val pelangganNama = pelanggan["nama"].toString()
            val trxId = newId("TRX")

            DatabaseService.insertTransaksi(
                id = trxId,
                itemId = itemId,
                itemNama = itemNama,
                pelangganNama = pelangganNama,
                tanggalSewa = LocalDate.now().toString(),
                durasiRencana = durasi,
                totalBiaya = hargaPerHari * durasi
            )
            DatabaseService.updateStatusItem(itemId, false)

            mapOf(
                "id" to trxId,
                "itemNama" to itemNama,
                "pelangganNama" to pelangganNama,
                "message" to "Transaksi sewa berhasil dicatat!"
            )
        }

        ctx.status(201).json(hasil)
    }

    app.post("/api/kembali") { ctx ->
        val req = ctx.parseBody<KembaliRequest>()

        val trxId = req.transaksiId?.trim()
        if (trxId.isNullOrEmpty()) throw InputTidakValidException("Transaksi wajib dipilih.")

        val hasil = synchronized(lock) {
            val trx = DatabaseService.getTransaksiById(trxId) ?: throw TransaksiTidakDitemukanException(trxId)
            if (trx["selesai"] == true)
                throw InputTidakValidException("Transaksi $trxId sudah dikembalikan sebelumnya.")

            val itemId = trx["itemId"].toString()
            val item = DatabaseService.getItemById(itemId) ?: throw ItemTidakDitemukanException(itemId)

            val hargaPerHari = (item["hargaSewa"] as Number).toInt()
            val tanggalSewa = LocalDate.parse(trx["tanggalSewa"].toString())
            val hariKembali = LocalDate.now()
            val hariAktual = maxOf(1L, ChronoUnit.DAYS.between(tanggalSewa, hariKembali))
            val totalBiaya = (hargaPerHari * hariAktual).toInt()

            DatabaseService.selesaikanTransaksi(trxId, hariKembali.toString(), totalBiaya)
            DatabaseService.updateStatusItem(itemId, true)

            mapOf(
                "itemNama" to trx["itemNama"].toString(),
                "totalBiaya" to totalBiaya,
                "message" to "Item berhasil dikembalikan!"
            )
        }

        ctx.json(hasil)
    }
}