package com.sewadulu.controller

import com.sewadulu.model.TambahItemRequest
import com.sewadulu.service.ItemService
import io.javalin.Javalin

/** Routing /api/items. Hanya menerima request, memanggil service, lalu mengirim JSON. */
class ItemController(private val service: ItemService) {

    fun registerRoutes(app: Javalin) {
        app.get("/api/items") { ctx ->
            val items = service.daftar(ctx.queryParam("tersedia"))
            ctx.json(items.map { it.toMap() })
        }

        app.post("/api/items") { ctx ->
            val item = service.tambah(ctx.parseBody<TambahItemRequest>())
            ctx.status(201).json(item.toMap())
        }
    }
}
