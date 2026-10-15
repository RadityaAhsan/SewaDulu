package com.sewadulu.controller

import com.sewadulu.exception.SewaDuluException
import io.javalin.Javalin

/** Penangkap exception global: aplikasi tidak crash, frontend menerima JSON {"error": "..."}. */
object ErrorHandler {
    fun daftarkan(app: Javalin) {
        app.exception(SewaDuluException::class.java) { e, ctx ->
            ctx.status(e.kode).json(mapOf("error" to e.message))
        }
        app.exception(Exception::class.java) { e, ctx ->
            e.printStackTrace()
            ctx.status(500).json(mapOf("error" to "Terjadi kesalahan pada server. Silakan coba lagi."))
        }
    }
}
