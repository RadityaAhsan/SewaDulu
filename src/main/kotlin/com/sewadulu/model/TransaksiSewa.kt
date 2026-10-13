package com.sewadulu.model

import java.time.LocalDate

data class TransaksiSewa(
    val id: String,
    val itemId: String,
    val itemNama: String,
    val hargaPerHari: Double,
    val pelangganId: String,
    val pelangganNama: String,
    val tanggalSewa: LocalDate,
    val durasiRencana: Int,
    var tanggalKembali: LocalDate? = null,
    var totalBiaya: Double? = null
) {
    val selesai: Boolean get() = tanggalKembali != null

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "itemId" to itemId,
        "itemNama" to itemNama,
        "pelangganId" to pelangganId,
        "pelangganNama" to pelangganNama,
        "tanggalSewa" to tanggalSewa.toString(),
        "durasiRencana" to durasiRencana,
        "tanggalKembali" to tanggalKembali?.toString(),
        "totalBiaya" to totalBiaya,
        "selesai" to selesai
    )
}
