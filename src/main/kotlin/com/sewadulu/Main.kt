package com.sewadulu

import com.sewadulu.endpoint.endpointdb
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location

fun main() {
    DatabaseService.init()

    val app = Javalin.create { config ->
        config.staticFiles.add("/public", Location.CLASSPATH) // Melayani index.html, app.js, style.css
    }

    // Mendaftarkan seluruh routing API endpoint
    endpointdb(app)

    app.start(7070)
    println("SewaDulu berjalan di http://localhost:7070")
}