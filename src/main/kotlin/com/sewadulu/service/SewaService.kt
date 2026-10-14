package com.sewadulu.service

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.sewadulu.config.MongoConfig
import com.sewadulu.exception.*
import com.sewadulu.model.*
import org.bson.Document
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SewaService {
    // Menghubungkan ke koleksi database MongoDB
    private val itemCollection: MongoCollection<Document> = MongoConfig.database.getCollection("items")
    private val pelangganCollection: MongoCollection<Document> = MongoConfig.database.getCollection("pelanggan")
    private val transaksiCollection: MongoCollection<Document> = MongoConfig.database.getCollection("transaksi")

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
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

        // Generate ID unik sederhana berdasarkan waktu atau hitung jumlah data
        val count = itemCollection.countDocuments() + 1
        val id = "ITM-%03d".format(count)

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

        // Simpan ke MongoDB sebagai Document
        val doc = Document("id", item.id)
            .append("nama", item.nama)
            .append("hargaSewa", item.hargaSewa)
            .append("statusTersedia", item.statusTersedia)
            .append("kategori", kategori)

        when (item) {
            is Tenda -> doc.append("kapasitas", item.kapasitas)
            is Kamera -> doc.append("resolusi", item.resolusi)
            is Sepeda -> doc.append("jenisGigi", item.jenisGigi)
        }

        itemCollection.insertOne(doc)
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

        // Cek email duplikat di MongoDB
        val existing = pelangganCollection.find(Filters.eq("email", email)).first()
        if (existing != null) {
            throw InputTidakValidException("Email '$email' sudah terdaftar.")
        }

        val count = pelangganCollection.countDocuments() + 1
        val id = "PLG-%03d".format(count)
        val pelanggan = Pelanggan(id, nama, email, noHp)

        val doc = Document("id", pelanggan.id)
            .append("nama", pelanggan.nama)
            .append("email", pelanggan.email)
            .append("noHp", pelanggan.noHp)

        pelangganCollection.insertOne(doc)
        return pelanggan
    }

    @Synchronized
    fun daftarPelanggan(): List<Pelanggan> {
        val list = mutableListOf<Pelanggan>()
        for (doc in pelangganCollection.find()) {
            list.add(
                Pelanggan(
                    id = doc.getString("id") ?: "",
                    nama = doc.getString("nama") ?: "",
                    email = doc.getString("email") ?: "",
                    noHp = doc.getString("noHp") ?: ""
                )
            )
        }
        return list
    }

    // ---------- FITUR 3: Sewa Item ----------
    @Synchronized
    fun sewaItem(req: SewaRequest): TransaksiSewa {
        val itemId = req.itemId?.trim()
        if (itemId.isNullOrEmpty()) throw InputTidakValidException("Item wajib dipilih.")

        val pelangganId = req.pelangganId?.trim()
        if (pelangganId.isNullOrEmpty()) throw InputTidakValidException("Pelanggan wajib dipilih.")

        val durasi = req.durasiHari
        if (durasi == null || durasi <= 0) throw InputTidakValidException("Durasi sewa minimal 1 hari.")

        // Cari item di MongoDB
        val itemDoc = itemCollection.find(Filters.eq("id", itemId)).first()
            ?: throw ItemTidakDitemukanException(itemId)

        // Cari pelanggan di MongoDB
        val pelDoc = pelangganCollection.find(Filters.eq("id", pelangganId)).first()
            ?: throw PelangganTidakDitemukanException(pelangganId)

        val statusTersedia = itemDoc.getBoolean("statusTersedia") ?: true
        if (!statusTersedia) {
            throw ItemTidakTersediaException(itemDoc.getString("nama") ?: itemId)
        }

        // Update status item jadi tidak tersedia di MongoDB
        itemCollection.updateOne(Filters.eq("id", itemId), Document("\$set", Document("statusTersedia", false)))

        val count = transaksiCollection.countDocuments() + 1
        val trxId = "TRX-%03d".format(count)

        val transaksi = TransaksiSewa(
            id = trxId,
            itemId = itemId,
            itemNama = itemDoc.getString("nama") ?: "",
            hargaPerHari = itemDoc.getDouble("hargaSewa") ?: 0.0,
            pelangganId = pelangganId,
            pelangganNama = pelDoc.getString("nama") ?: "",
            tanggalSewa = LocalDate.now(),
            durasiRencana = durasi
        )

        val trxDoc = Document("id", transaksi.id)
            .append("itemId", transaksi.itemId)
            .append("itemNama", transaksi.itemNama)
            .append("hargaPerHari", transaksi.hargaPerHari)
            .append("pelangganId", transaksi.pelangganId)
            .append("pelangganNama", transaksi.pelangganNama)
            .append("tanggalSewa", transaksi.tanggalSewa.toString())
            .append("durasiRencana", transaksi.durasiRencana)
            .append("selesai", transaksi.selesai)

        transaksiCollection.insertOne(trxDoc)
        return transaksi
    }

    // ---------- FITUR 4: Kembalikan Item ----------
    @Synchronized
    fun kembalikanItem(req: KembaliRequest): TransaksiSewa {
        val trxId = req.transaksiId?.trim()
        if (trxId.isNullOrEmpty()) throw InputTidakValidException("Transaksi wajib dipilih.")

        val trxDoc = transaksiCollection.find(Filters.eq("id", trxId)).first()
            ?: throw TransaksiTidakDitemukanException(trxId)

        // Cek apakah sudah dikembalikan sebelumnya berdasarkan tanggalKembali di database
        if (trxDoc.getString("tanggalKembali") != null) {
            throw InputTidakValidException("Transaksi $trxId sudah dikembalikan sebelumnya.")
        }

        val itemId = trxDoc.getString("itemId") ?: ""
        val itemDoc = itemCollection.find(Filters.eq("id", itemId)).first()
            ?: throw ItemTidakDitemukanException(itemId)

        val tanggalSewaStr = trxDoc.getString("tanggalSewa") ?: LocalDate.now().toString()
        val tanggalSewa = LocalDate.parse(tanggalSewaStr)
        val hariKembali = LocalDate.now()
        val hariAktual = maxOf(1L, ChronoUnit.DAYS.between(tanggalSewa, hariKembali))

        val hargaPerHari = trxDoc.getDouble("hargaPerHari") ?: 0.0
        val totalBiaya = hargaPerHari * hariAktual

        // Update transaksi di MongoDB (cukup simpan tanggalKembali dan totalBiaya)
        transaksiCollection.updateOne(
            Filters.eq("id", trxId),
            Document("\$set", Document("tanggalKembali", hariKembali.toString())
                .append("totalBiaya", totalBiaya))
        )

        // Update status item kembali tersedia di MongoDB
        itemCollection.updateOne(Filters.eq("id", itemId), Document("\$set", Document("statusTersedia", true)))

        return TransaksiSewa(
            id = trxId,
            itemId = itemId,
            itemNama = trxDoc.getString("itemNama") ?: "",
            hargaPerHari = hargaPerHari,
            pelangganId = trxDoc.getString("pelangganId") ?: "",
            pelangganNama = trxDoc.getString("pelangganNama") ?: "",
            tanggalSewa = tanggalSewa,
            durasiRencana = trxDoc.getInteger("durasiRencana") ?: 1
        ).apply {
            this.tanggalKembali = hariKembali
            this.totalBiaya = totalBiaya
        }
    }

    // ---------- FITUR 5: Daftar Item (filter ketersediaan) ----------
    @Synchronized
    fun daftarItem(filterTersedia: String?): List<ItemSewa> {
        val queryFilter = when (filterTersedia?.trim()?.lowercase()) {
            null, "", "semua" -> Document()
            "true" -> Filters.eq("statusTersedia", true)
            "false" -> Filters.eq("statusTersedia", false)
            else -> throw InputTidakValidException("Filter tidak valid. Gunakan: true, false, atau kosong.")
        }

        val list = mutableListOf<ItemSewa>()
        for (doc in itemCollection.find(queryFilter)) {
            val id = doc.getString("id") ?: ""
            val nama = doc.getString("nama") ?: ""
            val harga = doc.getDouble("hargaSewa") ?: 0.0
            val tersedia = doc.getBoolean("statusTersedia") ?: true
            val kategori = doc.getString("kategori") ?: ""

            val item: ItemSewa = when (kategori) {
                "tenda" -> Tenda(id, nama, harga, doc.getInteger("kapasitas") ?: 0).apply { statusTersedia = tersedia }
                "kamera" -> Kamera(id, nama, harga, doc.getString("resolusi") ?: "").apply { statusTersedia = tersedia }
                "sepeda" -> Sepeda(id, nama, harga, doc.getString("jenisGigi") ?: "").apply { statusTersedia = tersedia }
                else -> Tenda(id, nama, harga, 0).apply { statusTersedia = tersedia }
            }
            list.add(item)
        }
        return list
    }

    @Synchronized
    fun daftarTransaksi(): List<TransaksiSewa> {
        val list = mutableListOf<TransaksiSewa>()
        for (doc in transaksiCollection.find()) {
            val trx = TransaksiSewa(
                id = doc.getString("id") ?: "",
                itemId = doc.getString("itemId") ?: "",
                itemNama = doc.getString("itemNama") ?: "",
                hargaPerHari = doc.getDouble("hargaPerHari") ?: 0.0,
                pelangganId = doc.getString("pelangganId") ?: "",
                pelangganNama = doc.getString("pelangganNama") ?: "",
                tanggalSewa = LocalDate.parse(doc.getString("tanggalSewa") ?: LocalDate.now().toString()),
                durasiRencana = doc.getInteger("durasiRencana") ?: 1
            )

            doc.getString("tanggalKembali")?.let {
                trx.tanggalKembali = LocalDate.parse(it)
            }
            doc.getDouble("totalBiaya")?.let {
                trx.totalBiaya = it
            }
            list.add(trx)
        }
        return list
    }
}