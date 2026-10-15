package com.sewadulu.service

import com.sewadulu.exception.InputTidakValidException
import com.sewadulu.model.*
import com.sewadulu.repository.ItemRepository
import com.sewadulu.util.IdGenerator
import com.sewadulu.util.Validasi

/** Logika bisnis item: validasi, pembuatan objek Tenda/Kamera/Sepeda, filter ketersediaan. */
class ItemService(private val itemRepository: ItemRepository) {

    @Synchronized
    fun tambah(req: TambahItemRequest): ItemSewa {
        val kategori = req.kategori?.trim()?.lowercase()
        if (kategori.isNullOrEmpty()) throw InputTidakValidException("Kategori wajib dipilih.")

        val nama = req.nama?.trim()
        if (nama.isNullOrEmpty()) throw InputTidakValidException("Nama item tidak boleh kosong.")

        val harga = req.hargaSewa
        if (harga == null || harga <= 0) throw InputTidakValidException("Harga sewa harus diisi dan lebih dari 0.")

        val id = IdGenerator.baru("ITM")

        val item: ItemSewa = when (kategori) {
            "tenda" -> {
                val kapasitas = req.kapasitas
                if (kapasitas == null || kapasitas <= 0)
                    throw InputTidakValidException("Kapasitas tenda harus diisi dan lebih dari 0.")
                Tenda(id, nama, harga, kapasitas)
            }
            "kamera" -> {
                val resolusi = req.resolusi?.trim()
                if (resolusi.isNullOrEmpty())
                    throw InputTidakValidException("Resolusi kamera tidak boleh kosong.")
                val teks = Validasi.formatDenganSatuan(resolusi, "MP")
                    ?: throw InputTidakValidException("Resolusi kamera harus berupa angka (contoh: 24).")
                Kamera(id, nama, harga, teks)
            }
            "sepeda" -> {
                val gigi = req.jenisGigi?.trim()
                if (gigi.isNullOrEmpty())
                    throw InputTidakValidException("Jenis gigi sepeda tidak boleh kosong.")
                val teks = Validasi.formatDenganSatuan(gigi, "Speed")
                    ?: throw InputTidakValidException("Jenis gigi sepeda harus berupa angka (contoh: 21).")
                Sepeda(id, nama, harga, teks)
            }
            else -> throw InputTidakValidException("Kategori '$kategori' tidak dikenal (pilih: tenda, kamera, sepeda).")
        }

        itemRepository.simpan(item)
        return item
    }

    fun daftar(filterTersedia: String?): List<ItemSewa> {
        val semua = itemRepository.semua()
        return when (filterTersedia?.trim()?.lowercase()) {
            null, "", "semua" -> semua
            "true" -> semua.filter { it.statusTersedia }
            "false" -> semua.filter { !it.statusTersedia }
            else -> throw InputTidakValidException("Filter tidak valid. Gunakan: true, false, atau kosong.")
        }
    }
}
