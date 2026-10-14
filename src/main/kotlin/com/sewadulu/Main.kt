package com.sewadulu

import com.sewadulu.controller.SewaController
import com.sewadulu.service.SewaService
import io.javalin.Javalin

fun main() {
    // 1. Jalankan server Javalin (Koneksi MongoDB akan otomatis aktif saat Service dipanggil)
    val app = Javalin.create { config ->
        config.staticFiles.add("/private")
    }.start(7070)

    // 2. Inisialisasi Service & Controller
    val sewaService = SewaService()
    val sewaController = SewaController(sewaService)

    // 3. Daftarkan routes
    sewaController.registerRoutes(app)
}