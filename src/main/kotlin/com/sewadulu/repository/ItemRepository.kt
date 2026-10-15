package com.sewadulu.repository

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.sewadulu.config.MongoConfig
import com.sewadulu.model.ItemSewa
import com.sewadulu.model.Kamera
import com.sewadulu.model.Sepeda
import com.sewadulu.model.Tenda
import com.sewadulu.util.Validasi
import org.bson.Document

/** Akses data koleksi "items". Hanya berurusan dengan MongoDB, tanpa aturan bisnis. */
class ItemRepository {
    private val collection: MongoCollection<Document>
        get() = MongoConfig.database.getCollection("items")

    fun simpan(item: ItemSewa) {
        val doc = Document("id", item.id)
            .append("kategori", item.kategori)
            .append("nama", item.nama)
            .append("hargaSewa", item.hargaSewa)
            .append("statusTersedia", item.statusTersedia)

        when (item) {
            is Tenda -> doc.append("kapasitas", item.kapasitas)
            is Kamera -> doc.append("resolusi", item.resolusi)
            is Sepeda -> doc.append("jenisGigi", item.jenisGigi)
        }
        collection.insertOne(doc)
    }

    fun semua(): List<ItemSewa> = collection.find().mapNotNull { it.toItem() }

    fun cariById(id: String): ItemSewa? =
        collection.find(Filters.eq("id", id)).first()?.toItem()

    fun ubahStatus(id: String, tersedia: Boolean) {
        collection.updateOne(
            Filters.eq("id", id),
            Document("\$set", Document("statusTersedia", tersedia))
        )
    }

    // Document -> objek domain. Dibuat toleran terhadap data lama (angka / teks).
    private fun Document.toItem(): ItemSewa? {
        val id = getString("id") ?: ""
        val nama = getString("nama") ?: ""
        val harga = (get("hargaSewa") as? Number)?.toDouble() ?: 0.0

        val item: ItemSewa = when ((getString("kategori") ?: "").lowercase()) {
            "tenda" -> Tenda(id, nama, harga, angka("kapasitas"))
            "kamera" -> Kamera(id, nama, harga, teksDenganSatuan("resolusi", "MP"))
            "sepeda" -> Sepeda(id, nama, harga, teksDenganSatuan("jenisGigi", "Speed"))
            else -> return null
        }
        item.statusTersedia = getBoolean("statusTersedia") ?: true
        return item
    }

    private fun Document.angka(kunci: String): Int = when (val v = get(kunci)) {
        is Number -> v.toInt()
        is String -> Regex("\\d+").find(v)?.value?.toInt() ?: 0
        else -> 0
    }

    private fun Document.teksDenganSatuan(kunci: String, satuan: String): String {
        val mentah = get(kunci)?.toString() ?: ""
        return Validasi.formatDenganSatuan(mentah, satuan) ?: mentah
    }
}
