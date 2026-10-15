package com.sewadulu.repository

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.sewadulu.config.MongoConfig
import com.sewadulu.model.Pelanggan
import org.bson.Document
import java.util.regex.Pattern

/** Akses data koleksi "pelanggan". */
class PelangganRepository {
    private val collection: MongoCollection<Document>
        get() = MongoConfig.database.getCollection("pelanggan")

    fun simpan(pelanggan: Pelanggan) {
        collection.insertOne(
            Document("id", pelanggan.id)
                .append("nama", pelanggan.nama)
                .append("email", pelanggan.email)
                .append("noHp", pelanggan.noHp)
        )
    }

    fun semua(): List<Pelanggan> = collection.find().map { it.toPelanggan() }.toList()

    fun cariById(id: String): Pelanggan? =
        collection.find(Filters.eq("id", id)).first()?.toPelanggan()

    /** Pengecekan email tidak membedakan huruf besar/kecil. */
    fun emailSudahTerdaftar(email: String): Boolean {
        val filter = Filters.regex("email", "^" + Pattern.quote(email) + "$", "i")
        return collection.find(filter).first() != null
    }

    private fun Document.toPelanggan() = Pelanggan(
        id = getString("id") ?: "",
        nama = getString("nama") ?: "",
        email = getString("email") ?: "",
        noHp = getString("noHp") ?: ""
    )
}
