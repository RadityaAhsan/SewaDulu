package com.sewadulu.model

/**
 * Abstract class induk untuk semua barang yang bisa disewa.
 */
abstract class ItemSewa(
    val id: String,
    val nama: String,
    val hargaSewa: Double,        // harga per hari
    var statusTersedia: Boolean = true
) {
    /** Nama kategori, diimplementasikan oleh tiap turunan. */
    abstract val kategori: String

    /** Atribut unik tiap turunan (polimorfisme). */
    abstract fun atributUnik(): Map<String, Any>

    /** Representasi untuk response JSON. */
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "kategori" to kategori,
        "nama" to nama,
        "hargaSewa" to hargaSewa,
        "statusTersedia" to statusTersedia,
        "detail" to atributUnik()
    )
}

class Tenda(
    id: String, nama: String, hargaSewa: Double,
    val kapasitas: Int
) : ItemSewa(id, nama, hargaSewa) {
    override val kategori = "Tenda"
    override fun atributUnik(): Map<String, Any> = mapOf("kapasitas" to "$kapasitas orang")
}

class Kamera(
    id: String, nama: String, hargaSewa: Double,
    val resolusi: String
) : ItemSewa(id, nama, hargaSewa) {
    override val kategori = "Kamera"
    override fun atributUnik(): Map<String, Any> = mapOf("resolusi" to resolusi)
}

class Sepeda(
    id: String, nama: String, hargaSewa: Double,
    val jenisGigi: String
) : ItemSewa(id, nama, hargaSewa) {
    override val kategori = "Sepeda"
    override fun atributUnik(): Map<String, Any> = mapOf("jenisGigi" to jenisGigi)
}
