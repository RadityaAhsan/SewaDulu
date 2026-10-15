package com.sewadulu.controller

import com.sewadulu.exception.InputTidakValidException
import io.javalin.http.Context

/** JSON rusak / tipe data salah diubah menjadi exception khusus aplikasi. */
inline fun <reified T> Context.parseBody(): T =
    try {
        this.bodyAsClass(T::class.java)
    } catch (e: Exception) {
        throw InputTidakValidException("Format data tidak valid. Periksa kembali isian Anda.")
    }
