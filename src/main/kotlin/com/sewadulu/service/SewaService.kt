package com.sewadulu.service

import com.sewadulu.exception.*
import com.sewadulu.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SewaService {

    // Collection sesuai ketentuan: MutableMap untuk pencarian cepat via ID,
    // MutableList untuk riwayat transaksi.
    private val daftarItem: MutableMap<String, ItemSewa> = mutableMapOf()
    private val daftarPelanggan: MutableMap<String, Pelanggan> = mutableMapOf()
    private val riwayatTransaksi: MutableList<TransaksiSewa> = mutableListOf()

    private var seqItem = 0
    private var seqPelanggan = 0
    private var seqTransaksi = 0

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    // Format nomor HP Indonesia: 08xx / 628xx / +628xx
    private val hpRegex = Regex("^(\\+62|62|0)8[0-9]{8,11}$")

    // ---------- FITUR 1: Tambah Item ----------
    @Synchronized
    fun tambahItem(req: TambahItemRequest): ItemSewa {
        val kategori = req.kategori?.trim()?.lowercase()
        if (kategori.isNullOrEmpty()) throw InputTidakValidException("Kategori wajib dipilih.")

        val nama = req.nama?.trim()
        if (nama.isNullOrEmpty()) throw InputTidakValidException("Nama item tidak boleh kosong.")

        val harga = req.hargaSewa
        if (harga == null || harga <= 0) throw InputTidakValidException("Harga sewa harus diisi dan lebih dari 0.")

        val id = "ITM-%03d".format(seqItem + 1)

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
                Kamera(id, nama, harga, resolusi)
            }
            "sepeda" -> {
                val gigi = req.jenisGigi?.trim()
                if (gigi.isNullOrEmpty())
                    throw InputTidakValidException("Jenis gigi sepeda tidak boleh kosong.")
                Sepeda(id, nama, harga, gigi)
            }
            else -> throw InputTidakValidException("Kategori '$kategori' tidak dikenal (pilih: tenda, kamera, sepeda).")
        }

        seqItem++
        daftarItem[id] = item
        return item
    }

    // ---------- FITUR 2: Tambah Pelanggan ----------
    @Synchronized
    fun tambahPelanggan(req: TambahPelangganRequest): Pelanggan {
        val nama = req.nama?.trim()
        if (nama.isNullOrEmpty()) throw InputTidakValidException("Nama pelanggan tidak boleh kosong.")

        val email = req.email?.trim()
        if (email.isNullOrEmpty() || !emailRegex.matches(email))
            throw InputTidakValidException("Format email tidak valid (contoh: nama@email.com).")

        val noHp = req.noHp?.trim()?.replace(" ", "")?.replace("-", "")
        if (noHp.isNullOrEmpty() || !hpRegex.matches(noHp))
            throw InputTidakValidException("Format nomor HP tidak valid (contoh: 081234567890).")

        if (daftarPelanggan.values.any { it.email.equals(email, ignoreCase = true) })
            throw InputTidakValidException("Email '$email' sudah terdaftar.")

        val id = "PLG-%03d".format(++seqPelanggan)
        val pelanggan = Pelanggan(id, nama, email, noHp)
        daftarPelanggan[id] = pelanggan
        return pelanggan
    }

    @Synchronized
    fun daftarPelanggan(): List<Pelanggan> = daftarPelanggan.values.toList()

    // ---------- FITUR 3: Sewa Item ----------
    @Synchronized
    fun sewaItem(req: SewaRequest): TransaksiSewa {
        val itemId = req.itemId?.trim()
        if (itemId.isNullOrEmpty()) throw InputTidakValidException("Item wajib dipilih.")
        val pelangganId = req.pelangganId?.trim()
        if (pelangganId.isNullOrEmpty()) throw InputTidakValidException("Pelanggan wajib dipilih.")
        val durasi = req.durasiHari
        if (durasi == null || durasi <= 0) throw InputTidakValidException("Durasi sewa minimal 1 hari.")

        val item = daftarItem[itemId] ?: throw ItemTidakDitemukanException(itemId)
        val pelanggan = daftarPelanggan[pelangganId] ?: throw PelangganTidakDitemukanException(pelangganId)

        if (!item.statusTersedia) throw ItemTidakTersediaException(item.nama)

        item.statusTersedia = false
        val transaksi = TransaksiSewa(
            id = "TRX-%03d".format(++seqTransaksi),
            itemId = item.id,
            itemNama = item.nama,
            hargaPerHari = item.hargaSewa,
            pelangganId = pelanggan.id,
            pelangganNama = pelanggan.nama,
            tanggalSewa = LocalDate.now(),
            durasiRencana = durasi
        )
        riwayatTransaksi.add(transaksi)
        return transaksi
    }

    // ---------- FITUR 4: Kembalikan Item ----------
    @Synchronized
    fun kembalikanItem(req: KembaliRequest): TransaksiSewa {
        val trxId = req.transaksiId?.trim()
        if (trxId.isNullOrEmpty()) throw InputTidakValidException("Transaksi wajib dipilih.")

        val transaksi = riwayatTransaksi.find { it.id == trxId }
            ?: throw TransaksiTidakDitemukanException(trxId)
        if (transaksi.selesai) throw InputTidakValidException("Transaksi $trxId sudah dikembalikan sebelumnya.")

        val item = daftarItem[transaksi.itemId] ?: throw ItemTidakDitemukanException(transaksi.itemId)

        val hariKembali = LocalDate.now()
        val hariAktual = maxOf(1L, ChronoUnit.DAYS.between(transaksi.tanggalSewa, hariKembali))

        transaksi.tanggalKembali = hariKembali
        transaksi.totalBiaya = transaksi.hargaPerHari * hariAktual
        item.statusTersedia = true
        return transaksi
    }

    // ---------- FITUR 5: Daftar Item (filter ketersediaan) ----------
    @Synchronized
    fun daftarItem(filterTersedia: String?): List<ItemSewa> {
        val semua = daftarItem.values.toList()
        return when (filterTersedia?.trim()?.lowercase()) {
            null, "", "semua" -> semua
            "true" -> semua.filter { it.statusTersedia }
            "false" -> semua.filter { !it.statusTersedia }
            else -> throw InputTidakValidException("Filter tidak valid. Gunakan: true, false, atau kosong.")
        }
    }

    @Synchronized
    fun daftarTransaksi(): List<TransaksiSewa> = riwayatTransaksi.toList()
}
