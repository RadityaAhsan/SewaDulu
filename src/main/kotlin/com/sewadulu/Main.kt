package com.sewadulu

import com.sewadulu.endpoint.endpointdb
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location

fun main() {
    DatabaseService.init()

    val app = Javalin.create { config ->
        config.staticFiles.add("/public", Location.CLASSPATH)   // melayani index.html
    }

    // Semua route (items, pelanggan, sewa, kembali, transaksi) membaca/menulis ke SQLite.
    // Harus didaftarkan SEBELUM app.start().
    endpointdb(app)

    app.start(7070)
    println("SewaDulu berjalan di http://localhost:7070")
}