package com.sewadulu.repository

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.sewadulu.config.MongoConfig
import com.sewadulu.model.TransaksiSewa
import org.bson.Document
import java.time.LocalDate

/** Akses data koleksi "transaksi". */
class TransaksiRepository {
    private val collection: MongoCollection<Document>
        get() = MongoConfig.database.getCollection("transaksi")

    fun simpan(trx: TransaksiSewa) {
        collection.insertOne(
            Document("id", trx.id)
                .append("itemId", trx.itemId)
                .append("itemNama", trx.itemNama)
                .append("hargaPerHari", trx.hargaPerHari)
                .append("pelangganId", trx.pelangganId)
                .append("pelangganNama", trx.pelangganNama)
                .append("tanggalSewa", trx.tanggalSewa.toString())
                .append("durasiRencana", trx.durasiRencana)
                .append("selesai", false)
        )
    }

    /** Transaksi terbaru berada di urutan paling atas. */
    fun semua(): List<TransaksiSewa> = collection.find().map { it.toTransaksi() }.toList().reversed()

    fun cariById(id: String): TransaksiSewa? =
        collection.find(Filters.eq("id", id)).first()?.toTransaksi()

    fun selesaikan(id: String, tanggalKembali: LocalDate, totalBiaya: Double) {
        collection.updateOne(
            Filters.eq("id", id),
            Document(
                "\$set",
                Document("tanggalKembali", tanggalKembali.toString())
                    .append("totalBiaya", totalBiaya)
                    .append("selesai", true)
            )
        )
    }

    // Toleran terhadap data lama: field bisa kosong / bertipe berbeda.
    private fun Document.toTransaksi(): TransaksiSewa {
        val trx = TransaksiSewa(
            id = getString("id") ?: "",
            itemId = getString("itemId") ?: "",
            itemNama = getString("itemNama") ?: "",
            hargaPerHari = (get("hargaPerHari") as? Number)?.toDouble() ?: 0.0,
            pelangganId = getString("pelangganId") ?: "",
            pelangganNama = getString("pelangganNama") ?: "",
            tanggalSewa = LocalDate.parse(getString("tanggalSewa") ?: LocalDate.now().toString()),
            durasiRencana = (get("durasiRencana") as? Number)?.toInt() ?: 1
        )
        val kembali = getString("tanggalKembali")
        if (!kembali.isNullOrBlank() && kembali != "null") {
            trx.tanggalKembali = LocalDate.parse(kembali)
            trx.totalBiaya = (get("totalBiaya") as? Number)?.toDouble()
        }
        return trx
    }
}
