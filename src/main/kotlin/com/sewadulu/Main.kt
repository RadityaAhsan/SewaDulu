package com.sewadulu

import com.sewadulu.controller.SewaController
import com.sewadulu.service.SewaService
import io.javalin.Javalin

fun main() {
    val app = Javalin.create { config ->
        config.staticFiles.add("/public")
    }

    // 1. Buat dulu SewaService-nya
    val sewaService = SewaService()

    // 2. Masukkan sewaService ke dalam SewaController
    val sewaController = SewaController(sewaService)

    // 3. Panggil fungsi registerRoutes (sesuai nama buatan Fahmi)
    sewaController.registerRoutes(app)

    app.start(7070)
}