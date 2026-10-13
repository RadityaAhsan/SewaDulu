package com.sewadulu.model

// Semua field nullable agar validasi dilakukan di service, bukan crash saat parsing.
data class TambahItemRequest(
    val kategori: String? = null,
    val nama: String? = null,
    val hargaSewa: Double? = null,
    val kapasitas: Int? = null,
    val resolusi: String? = null,
    val jenisGigi: String? = null
)

data class TambahPelangganRequest(
    val nama: String? = null,
    val email: String? = null,
    val noHp: String? = null
)

data class SewaRequest(
    val itemId: String? = null,
    val pelangganId: String? = null,
    val durasiHari: Int? = null
)

data class KembaliRequest(
    val transaksiId: String? = null
)
