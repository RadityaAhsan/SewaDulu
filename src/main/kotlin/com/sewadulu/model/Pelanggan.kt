package com.sewadulu.model

data class Pelanggan(
    val id: String,
    val nama: String,
    val email: String,
    val noHp: String
) {
    /** Representasi untuk response JSON. */
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "nama" to nama,
        "email" to email,
        "noHp" to noHp
    )
}
