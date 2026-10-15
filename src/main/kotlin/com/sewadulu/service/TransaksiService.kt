package com.sewadulu.service

import com.sewadulu.exception.*
import com.sewadulu.model.KembaliRequest
import com.sewadulu.model.SewaRequest
import com.sewadulu.model.TransaksiSewa
import com.sewadulu.repository.ItemRepository
import com.sewadulu.repository.PelangganRepository
import com.sewadulu.repository.TransaksiRepository
import com.sewadulu.util.IdGenerator
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Logika bisnis penyewaan: sewa item, pengembalian, dan perhitungan biaya. */
class TransaksiService(
    private val itemRepository: ItemRepository,
    private val pelangganRepository: PelangganRepository,
    private val transaksiRepository: TransaksiRepository
) {

    @Synchronized
    fun sewa(req: SewaRequest): TransaksiSewa {
        val itemId = req.itemId?.trim()
        if (itemId.isNullOrEmpty()) throw InputTidakValidException("Item wajib dipilih.")

        val pelangganId = req.pelangganId?.trim()
        if (pelangganId.isNullOrEmpty()) throw InputTidakValidException("Pelanggan wajib dipilih.")

        val durasi = req.durasiHari
        if (durasi == null || durasi <= 0) throw InputTidakValidException("Durasi sewa minimal 1 hari.")

        val item = itemRepository.cariById(itemId) ?: throw ItemTidakDitemukanException(itemId)
        val pelanggan = pelangganRepository.cariById(pelangganId)
            ?: throw PelangganTidakDitemukanException(pelangganId)

        if (!item.statusTersedia) throw ItemTidakTersediaException(item.nama)

        val trx = TransaksiSewa(
            id = IdGenerator.baru("TRX"),
            itemId = item.id,
            itemNama = item.nama,
            hargaPerHari = item.hargaSewa,
            pelangganId = pelanggan.id,
            pelangganNama = pelanggan.nama,
            tanggalSewa = LocalDate.now(),
            durasiRencana = durasi
        )

        transaksiRepository.simpan(trx)
        itemRepository.ubahStatus(item.id, false)
        return trx
    }

    @Synchronized
    fun kembalikan(req: KembaliRequest): TransaksiSewa {
        val trxId = req.transaksiId?.trim()
        if (trxId.isNullOrEmpty()) throw InputTidakValidException("Transaksi wajib dipilih.")

        val trx = transaksiRepository.cariById(trxId) ?: throw TransaksiTidakDitemukanException(trxId)
        if (trx.selesai) throw InputTidakValidException("Transaksi $trxId sudah dikembalikan sebelumnya.")

        val item = itemRepository.cariById(trx.itemId) ?: throw ItemTidakDitemukanException(trx.itemId)

        // Transaksi lama mungkin belum menyimpan harga per hari, pakai harga item saat ini.
        val hargaPerHari = if (trx.hargaPerHari > 0) trx.hargaPerHari else item.hargaSewa
        val hariKembali = LocalDate.now()
        val totalBiaya = hitungBiaya(hargaPerHari, trx.tanggalSewa, hariKembali)

        transaksiRepository.selesaikan(trx.id, hariKembali, totalBiaya)
        itemRepository.ubahStatus(item.id, true)

        trx.tanggalKembali = hariKembali
        trx.totalBiaya = totalBiaya
        return trx
    }

    fun daftar(): List<TransaksiSewa> = transaksiRepository.semua()

    /** Biaya = harga per hari x hari aktual (minimal 1 hari). */
    private fun hitungBiaya(hargaPerHari: Double, tanggalSewa: LocalDate, tanggalKembali: LocalDate): Double {
        val hariAktual = maxOf(1L, ChronoUnit.DAYS.between(tanggalSewa, tanggalKembali))
        return hargaPerHari * hariAktual
    }
}
