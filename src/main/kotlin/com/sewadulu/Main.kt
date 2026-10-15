package com.sewadulu

import com.sewadulu.controller.ErrorHandler
import com.sewadulu.controller.ItemController
import com.sewadulu.controller.PelangganController
import com.sewadulu.controller.TransaksiController
import com.sewadulu.repository.ItemRepository
import com.sewadulu.repository.PelangganRepository
import com.sewadulu.repository.TransaksiRepository
import com.sewadulu.service.ItemService
import com.sewadulu.service.PelangganService
import com.sewadulu.service.TransaksiService
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location

/** Titik awal aplikasi: hanya merakit (wiring) lapisan dari bawah ke atas. */
fun main() {
    // Lapisan data
    val itemRepository = ItemRepository()
    val pelangganRepository = PelangganRepository()
    val transaksiRepository = TransaksiRepository()

    // Lapisan logika bisnis
    val itemService = ItemService(itemRepository)
    val pelangganService = PelangganService(pelangganRepository)
    val transaksiService = TransaksiService(itemRepository, pelangganRepository, transaksiRepository)

    val app = Javalin.create { config ->
        config.staticFiles.add("/public", Location.CLASSPATH) // index.html, app.js, style.css
    }

    // Lapisan controller
    ErrorHandler.daftarkan(app)
    ItemController(itemService).registerRoutes(app)
    PelangganController(pelangganService).registerRoutes(app)
    TransaksiController(transaksiService).registerRoutes(app)

    app.start(7070)
    println("SewaDulu berjalan di http://localhost:7070")
}
