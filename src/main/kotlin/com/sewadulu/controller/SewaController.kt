package com.sewadulu.controller

import com.sewadulu.exception.InputTidakValidException
import com.sewadulu.exception.SewaDuluException
import com.sewadulu.model.*
import com.sewadulu.service.SewaService
import io.javalin.Javalin
import io.javalin.http.Context

class SewaController(private val service: SewaService) {

    // Helper untuk menyesuaikan format item agar memiliki properti 'detail' yang dibaca oleh app.js
    private fun itemKeFrontend(item: ItemSewa): Map<String, Any> {
        val map = item.toMap()
        val kategori = (map["kategori"] as? String)?.lowercase() ?: ""

        val kunciDetail = when (kategori) {
            "tenda" -> "kapasitas"
            "kamera" -> "resolusi"
            "sepeda" -> "jenisGigi"
            else -> "info"
        }

        val nilaiDetail = when (kategori) {
            "tenda" -> "${map["kapasitas"]} orang"
            "kamera" -> map["resolusi"] ?: ""
            "sepeda" -> map["jenisGigi"] ?: ""
            else -> ""
        }

        return mapOf(
            "id" to (map["id"] ?: ""),
            "kategori" to (map["kategori"] ?: ""),
            "nama" to (map["nama"] ?: ""),
            "hargaSewa" to (map["hargaSewa"] ?: 0.0),
            "statusTersedia" to (map["statusTersedia"] ?: true),
            "detail" to mapOf(kunciDetail to nilaiDetail)
        )
    }

    fun registerRoutes(app: Javalin) {
        // Penangkap exception global agar aplikasi tidak crash dan mengirim pesan error bersih ke UI
        app.exception(SewaDuluException::class.java) { e, ctx ->
            ctx.status(e.kode).json(mapOf("error" to e.message))
        }
        app.exception(Exception::class.java) { e, ctx ->
            e.printStackTrace() // Mencetak log error asli ke konsol terminal untuk debugging
            ctx.status(500).json(mapOf("error" to "Terjadi kesalahan pada server. Silakan coba lagi."))
        }

        // Fitur 5: Daftar item dengan filter ketersediaan
        app.get("/api/items") { ctx ->
            val filter = ctx.queryParam("tersedia")
            val items = service.daftarItem(filter)
            ctx.json(items.map { itemKeFrontend(it) })
        }

        // Fitur 1: Tambah item baru
        app.post("/api/items") { ctx ->
            val item = service.tambahItem(ctx.parseBody<TambahItemRequest>())
            ctx.status(201).json(itemKeFrontend(item))
        }

        // Fitur 2: Daftar & Tambah Pelanggan
        app.get("/api/pelanggan") { ctx ->
            ctx.json(service.daftarPelanggan())
        }
        app.post("/api/pelanggan") { ctx ->
            ctx.status(201).json(service.tambahPelanggan(ctx.parseBody<TambahPelangganRequest>()))
        }

        // Fitur 3: Sewa item
        app.post("/api/sewa") { ctx ->
            ctx.status(201).json(service.sewaItem(ctx.parseBody<SewaRequest>()).toMap())
        }

        // Fitur 4: Pengembalian item
        app.post("/api/kembali") { ctx ->
            ctx.json(service.kembalikanItem(ctx.parseBody<KembaliRequest>()).toMap())
        }

        // Riwayat transaksi
        app.get("/api/transaksi") { ctx ->
            ctx.json(service.daftarTransaksi().map { trx ->
                val map = trx.toMap().toMutableMap()
                map["selesai"] = trx.tanggalKembali != null
                map
            })
        }
    }

    private inline fun <reified T> Context.parseBody(): T =
        try {
            this.bodyAsClass(T::class.java)
        } catch (e: Exception) {
            throw InputTidakValidException("Format data tidak valid. Periksa kembali isian Anda.")
        }
}