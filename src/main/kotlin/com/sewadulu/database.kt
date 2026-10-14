package com.sewadulu

import com.sewadulu.config.MongoConfig
import com.mongodb.client.MongoCollection
import org.bson.Document

object DatabaseService {

    private val itemCollection: MongoCollection<Document>
        get() = MongoConfig.database.getCollection("items")

    private val pelangganCollection: MongoCollection<Document>
        get() = MongoConfig.database.getCollection("pelanggan")

    private val transaksiCollection: MongoCollection<Document>
        get() = MongoConfig.database.getCollection("transaksi")

    fun init() {
        println("Koneksi ke MongoDB Atlas berhasil diinisialisasi!")
    }

    // ==========================================
    // 1. ITEM
    // ==========================================
    private fun Document.toItem(): Map<String, Any> {
        val kategori = getString("kategori") ?: ""

        // Membaca harga sewa secara aman untuk menghindari ClassCastException (Integer ke Double)
        val rawHarga = get("hargaSewa")
        val harga = when (rawHarga) {
            is Double -> rawHarga
            is Int -> rawHarga.toDouble()
            is Long -> rawHarga.toDouble()
            else -> 0.0
        }

        // Membungkus atribut unik ke dalam map "detail" agar sesuai dengan app.js
        val detailMap = when (kategori.lowercase()) {
            "tenda" -> mapOf("kapasitas" to (getString("kapasitas") ?: "${getInteger("kapasitas") ?: 0} orang"))
            "kamera" -> mapOf("resolusi" to (getString("resolusi") ?: ""))
            "sepeda" -> mapOf("jenisGigi" to (getString("jenisGigi") ?: ""))
            else -> mapOf("info" to (getString("detail") ?: ""))
        }

        return mapOf(
            "id" to (getString("id") ?: ""),
            "kategori" to kategori,
            "nama" to (getString("nama") ?: ""),
            "hargaSewa" to harga,
            "statusTersedia" to (getBoolean("statusTersedia") ?: true),
            "detail" to detailMap
        )
    }

    fun insertItem(id: String, kategori: String, nama: String, hargaSewa: Int, detail: String) {
        val doc = Document("id", id)
            .append("kategori", kategori)
            .append("nama", nama)
            .append("hargaSewa", hargaSewa.toDouble())
            .append("statusTersedia", true)

        when (kategori.lowercase()) {
            "tenda" -> doc.append("kapasitas", detail)
            "kamera" -> doc.append("resolusi", detail)
            "sepeda" -> doc.append("jenisGigi", detail)
            else -> doc.append("detail", detail)
        }

        itemCollection.insertOne(doc)
    }

    fun getAllItems(): List<Map<String, Any>> {
        return itemCollection.find().map { it.toItem() }.toList()
    }

    fun getItemById(id: String): Map<String, Any>? {
        return itemCollection.find(Document("id", id)).first()?.toItem()
    }

    fun updateStatusItem(id: String, tersedia: Boolean) {
        itemCollection.updateOne(
            Document("id", id),
            Document("\$set", Document("statusTersedia", tersedia))
        )
    }

    // ==========================================
    // 2. PELANGGAN
    // ==========================================
    private fun Document.toPelanggan(): Map<String, Any> = mapOf(
        "id" to (getString("id") ?: ""),
        "nama" to (getString("nama") ?: ""),
        "email" to (getString("email") ?: ""),
        "noHp" to (getString("noHp") ?: "")
    )

    fun insertPelanggan(id: String, nama: String, email: String, noHp: String) {
        val doc = Document("id", id)
            .append("nama", nama)
            .append("email", email)
            .append("noHp", noHp)
        pelangganCollection.insertOne(doc)
    }

    fun getAllPelanggan(): List<Map<String, Any>> {
        return pelangganCollection.find().map { it.toPelanggan() }.toList()
    }

    fun getPelangganById(id: String): Map<String, Any>? {
        return pelangganCollection.find(Document("id", id)).first()?.toPelanggan()
    }

    fun emailSudahTerdaftar(email: String): Boolean {
        val query = Document("email", Document("\$regex", "^$email\$").append("\$options", "i"))
        return pelangganCollection.find(query).first() != null
    }

    // ==========================================
    // 3. TRANSAKSI
    // ==========================================
    private fun Document.toTransaksi(): Map<String, Any> {
        val tglKembali = getString("tanggalKembali")
        val isSelesai = getBoolean("selesai") ?: (!tglKembali.isNullOrEmpty() && tglKembali != "null")

        // Membaca total biaya secara aman agar tidak terjadi error tipe data
        val rawTotal = get("totalBiaya")
        val total = when (rawTotal) {
            is Double -> rawTotal
            is Int -> rawTotal.toDouble()
            is Long -> rawTotal.toDouble()
            else -> 0.0
        }

        return mapOf(
            "id" to (getString("id") ?: ""),
            "itemId" to (getString("itemId") ?: ""),
            "itemNama" to (getString("itemNama") ?: ""),
            "pelangganNama" to (getString("pelangganNama") ?: ""),
            "tanggalSewa" to (getString("tanggalSewa") ?: ""),
            "durasiRencana" to (getInteger("durasiRencana") ?: 1),
            "tanggalKembali" to (tglKembali ?: ""),
            "totalBiaya" to total,
            "selesai" to isSelesai
        )
    }

    fun insertTransaksi(
        id: String, itemId: String, itemNama: String,
        pelangganNama: String, tanggalSewa: String,
        durasiRencana: Int, totalBiaya: Int
    ) {
        val doc = Document("id", id)
            .append("itemId", itemId)
            .append("itemNama", itemNama)
            .append("pelangganNama", pelangganNama)
            .append("tanggalSewa", tanggalSewa)
            .append("durasiRencana", durasiRencana)
            .append("tanggalKembali", "")
            .append("totalBiaya", totalBiaya.toDouble())
            .append("selesai", false)
        transaksiCollection.insertOne(doc)
    }

    fun getAllTransaksi(): List<Map<String, Any>> {
        return transaksiCollection.find().map { it.toTransaksi() }.toList().reversed()
    }

    fun getTransaksiById(id: String): Map<String, Any>? {
        return transaksiCollection.find(Document("id", id)).first()?.toTransaksi()
    }

    fun selesaikanTransaksi(id: String, tanggalKembali: String, totalBiaya: Int) {
        transaksiCollection.updateOne(
            Document("id", id),
            Document("\$set", Document("tanggalKembali", tanggalKembali)
                .append("totalBiaya", totalBiaya.toDouble())
                .append("selesai", true))
        )
    }
}