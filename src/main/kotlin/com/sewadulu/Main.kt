package com.sewadulu

import io.javalin.Javalin

fun main() {
    // Membuat dan menjalankan server di port 7070
    val app = Javalin.create().start(7070)

    // Endpoint dasar untuk mengetes server
    app.get("/") { ctx ->
        ctx.result("Server Sistem SewaDulu berhasil berjalan!")
    }

    // Catatan: Nanti routing untuk SewaController.kt buatan Fahmi akan dipanggil di bawah ini
}