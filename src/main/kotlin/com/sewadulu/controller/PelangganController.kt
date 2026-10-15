package com.sewadulu.controller

import com.sewadulu.model.TambahPelangganRequest
import com.sewadulu.service.PelangganService
import io.javalin.Javalin

/** Routing /api/pelanggan. */
class PelangganController(private val service: PelangganService) {

    fun registerRoutes(app: Javalin) {
        app.get("/api/pelanggan") { ctx ->
            ctx.json(service.daftar().map { it.toMap() })
        }

        app.post("/api/pelanggan") { ctx ->
            val pelanggan = service.tambah(ctx.parseBody<TambahPelangganRequest>())
            ctx.status(201).json(pelanggan.toMap())
        }
    }
}
