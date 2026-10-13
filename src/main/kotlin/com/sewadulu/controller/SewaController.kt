package com.sewadulu.controller

import com.sewadulu.exception.InputTidakValidException
import com.sewadulu.exception.SewaDuluException
import com.sewadulu.model.*
import com.sewadulu.service.SewaService
import io.javalin.Javalin
import io.javalin.http.Context

class SewaController(private val service: SewaService) {

    fun registerRoutes(app: Javalin) {
        // Penangkap exception: aplikasi tidak crash, frontend menerima JSON {error: "..."}
        app.exception(SewaDuluException::class.java) { e, ctx ->
            ctx.status(e.kode).json(mapOf("error" to e.message))
        }
        app.exception(Exception::class.java) { e, ctx ->
            e.printStackTrace()
            ctx.status(500).json(mapOf("error" to "Terjadi kesalahan pada server. Silakan coba lagi."))
        }

        // Fitur 5: daftar item (?tersedia=true|false)
        app.get("/api/items") { ctx ->
            ctx.json(service.daftarItem(ctx.queryParam("tersedia")).map { it.toMap() })
        }
        // Fitur 1: tambah item
        app.post("/api/items") { ctx ->
            val item = service.tambahItem(ctx.parseBody<TambahItemRequest>())
            ctx.status(201).json(item.toMap())
        }

        // Fitur 2: pelanggan
        app.get("/api/pelanggan") { ctx -> ctx.json(service.daftarPelanggan()) }
        app.post("/api/pelanggan") { ctx ->
            ctx.status(201).json(service.tambahPelanggan(ctx.parseBody<TambahPelangganRequest>()))
        }

        // Fitur 3: sewa
        app.post("/api/sewa") { ctx ->
            ctx.status(201).json(service.sewaItem(ctx.parseBody<SewaRequest>()).toMap())
        }

        // Fitur 4: kembalikan
        app.post("/api/kembali") { ctx ->
            ctx.json(service.kembalikanItem(ctx.parseBody<KembaliRequest>()).toMap())
        }

        // Riwayat transaksi
        app.get("/api/transaksi") { ctx -> ctx.json(service.daftarTransaksi().map { it.toMap() }) }
    }

    /** JSON rusak / tipe data salah diubah menjadi custom exception. */
    private inline fun <reified T> Context.parseBody(): T =
        try {
            this.bodyAsClass(T::class.java)
        } catch (e: Exception) {
            throw InputTidakValidException("Format data tidak valid. Periksa kembali isian Anda.")
        }
}
