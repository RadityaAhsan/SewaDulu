package com.sewadulu.exception

/**
 * Sealed class: semua exception bisnis aplikasi ini terdaftar di sini.
 * Properti `kode` dipakai controller sebagai HTTP status code.
 */
sealed class SewaDuluException(
    override val message: String,
    val kode: Int
) : RuntimeException(message)

class InputTidakValidException(pesan: String) :
    SewaDuluException(pesan, 400)

class ItemTidakDitemukanException(id: String) :
    SewaDuluException("Item dengan ID '$id' tidak ditemukan.", 404)

class PelangganTidakDitemukanException(id: String) :
    SewaDuluException("Pelanggan dengan ID '$id' tidak ditemukan.", 404)

class TransaksiTidakDitemukanException(id: String) :
    SewaDuluException("Transaksi dengan ID '$id' tidak ditemukan.", 404)

class ItemTidakTersediaException(nama: String) :
    SewaDuluException("Item '$nama' sedang disewa dan tidak tersedia.", 409)