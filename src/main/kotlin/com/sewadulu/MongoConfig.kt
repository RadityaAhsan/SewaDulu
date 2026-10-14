package com.sewadulu.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase

object MongoConfig {
    // Ganti URL connection string sesuai dengan MongoDB lokal atau MongoDB Atlas Anda
    private const val CONNECTION_STRING = "mongodb://localhost:27017"
    private const val DATABASE_NAME = "sewadulu_db"

    private val client: MongoClient by lazy {
        MongoClients.create(CONNECTION_STRING)
    }

    val database: MongoDatabase by lazy {
        client.getDatabase(DATABASE_NAME)
    }
}