package com.sewadulu.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase

object MongoConfig {
    // Memaksa mengambil dari environment variable atau string Atlas langsung tanpa ke localhost
    private val CONNECTION_STRING = System.getenv("MONGODB_URI")
        ?: "mongodb+srv://fahmihadifir01_db_user:y3LbcRefLua9OGUa@fahmi.oesknei.mongodb.net/?appName=Fahmi&compressors=zlib"

    private const val DATABASE_NAME = "sewadulu_db"

    private val client: MongoClient by lazy {
        MongoClients.create(CONNECTION_STRING)
    }

    val database: MongoDatabase by lazy {
        client.getDatabase(DATABASE_NAME)
    }
}