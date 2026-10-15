package com.sewadulu.controller

import com.sewadulu.model.KembaliRequest
import com.sewadulu.model.SewaRequest
import com.sewadulu.service.TransaksiService
import io.javalin.Javalin

/** Routing /api/transaksi, /api/sewa, dan /api/kembali. */
class TransaksiController(private val service: TransaksiService) {

    fun registerRoutes(app: Javalin) {
        app.get("/api/transaksi") { ctx ->
            ctx.json(service.daftar().map { it.toMap() })
        }

        app.post("/api/sewa") { ctx ->
            val trx = service.sewa(ctx.parseBody<SewaRequest>())
            ctx.status(201).json(trx.toMap())
        }

        app.post("/api/kembali") { ctx ->
            val trx = service.kembalikan(ctx.parseBody<KembaliRequest>())
            ctx.json(trx.toMap())
        }
    }
}
