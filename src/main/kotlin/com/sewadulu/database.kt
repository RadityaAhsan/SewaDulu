package com.sewadulu

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

object DatabaseService {
    private const val URL = "jdbc:sqlite:C:\\POLITEKNIK MANUFAKTUR BANDUNG\\PROJECT\\sewadulu.db"

    private fun getConnection(): Connection = DriverManager.getConnection(URL)

    // ==========================================
    // 1. INISIALISASI TABEL
    // ==========================================
    fun init() {
        getConnection().use { conn ->
            conn.createStatement().use { st ->
                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS items (
                        id TEXT PRIMARY KEY,
                        kategori TEXT,
                        nama TEXT,
                        hargaSewa INTEGER,
                        statusTersedia INTEGER,
                        detail TEXT
                    )
                    """
                )
                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS pelanggan (
                        id TEXT PRIMARY KEY,
                        nama TEXT,
                        email TEXT,
                        noHp TEXT
                    )
                    """
                )
                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS transaksi (
                        id TEXT PRIMARY KEY,
                        itemId TEXT,
                        itemNama TEXT,
                        pelangganNama TEXT,
                        tanggalSewa TEXT,
                        durasiRencana INTEGER,
                        tanggalKembali TEXT,
                        totalBiaya INTEGER,
                        selesai INTEGER
                    )
                    """
                )
            }
        }
        println("Database SQLite & semua tabel berhasil diinisialisasi!")
    }

    // ==========================================
    // 2. ITEM
    // ==========================================
    private fun ResultSet.toItem(): Map<String, Any> = mapOf(
        "id" to getString("id"),
        "kategori" to getString("kategori"),
        "nama" to getString("nama"),
        "hargaSewa" to getInt("hargaSewa"),
        "statusTersedia" to (getInt("statusTersedia") == 1),
        "detail" to (getString("detail") ?: "")
    )

    fun insertItem(id: String, kategori: String, nama: String, hargaSewa: Int, detail: String) {
        getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT INTO items (id, kategori, nama, hargaSewa, statusTersedia, detail) VALUES (?, ?, ?, ?, 1, ?)"
            ).use { ps ->
                ps.setString(1, id)
                ps.setString(2, kategori)
                ps.setString(3, nama)
                ps.setInt(4, hargaSewa)
                ps.setString(5, detail)
                ps.executeUpdate()
            }
        }
    }

    fun getAllItems(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        getConnection().use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT * FROM items ORDER BY rowid").use { rs ->
                    while (rs.next()) list.add(rs.toItem())
                }
            }
        }
        return list
    }

    fun getItemById(id: String): Map<String, Any>? =
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM items WHERE id = ?").use { ps ->
                ps.setString(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toItem() else null }
            }
        }

    fun updateStatusItem(id: String, tersedia: Boolean) {
        getConnection().use { conn ->
            conn.prepareStatement("UPDATE items SET statusTersedia = ? WHERE id = ?").use { ps ->
                ps.setInt(1, if (tersedia) 1 else 0)
                ps.setString(2, id)
                ps.executeUpdate()
            }
        }
    }

    // ==========================================
    // 3. PELANGGAN
    // ==========================================
    private fun ResultSet.toPelanggan(): Map<String, Any> = mapOf(
        "id" to getString("id"),
        "nama" to getString("nama"),
        "email" to (getString("email") ?: ""),
        "noHp" to (getString("noHp") ?: "")
    )

    fun insertPelanggan(id: String, nama: String, email: String, noHp: String) {
        getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT INTO pelanggan (id, nama, email, noHp) VALUES (?, ?, ?, ?)"
            ).use { ps ->
                ps.setString(1, id)
                ps.setString(2, nama)
                ps.setString(3, email)
                ps.setString(4, noHp)
                ps.executeUpdate()
            }
        }
    }

    fun getAllPelanggan(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        getConnection().use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT * FROM pelanggan ORDER BY rowid").use { rs ->
                    while (rs.next()) list.add(rs.toPelanggan())
                }
            }
        }
        return list
    }

    fun getPelangganById(id: String): Map<String, Any>? =
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM pelanggan WHERE id = ?").use { ps ->
                ps.setString(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toPelanggan() else null }
            }
        }

    fun emailSudahTerdaftar(email: String): Boolean =
        getConnection().use { conn ->
            conn.prepareStatement("SELECT 1 FROM pelanggan WHERE LOWER(email) = LOWER(?)").use { ps ->
                ps.setString(1, email)
                ps.executeQuery().use { rs -> rs.next() }
            }
        }

    // ==========================================
    // 4. TRANSAKSI
    // ==========================================
    private fun ResultSet.toTransaksi(): Map<String, Any> = mapOf(
        "id" to getString("id"),
        "itemId" to getString("itemId"),
        "itemNama" to getString("itemNama"),
        "pelangganNama" to getString("pelangganNama"),
        "tanggalSewa" to getString("tanggalSewa"),
        "durasiRencana" to getInt("durasiRencana"),
        "tanggalKembali" to (getString("tanggalKembali") ?: ""),
        "totalBiaya" to getInt("totalBiaya"),
        "selesai" to (getInt("selesai") == 1)
    )

    /** tanggalKembali dibiarkan NULL sampai item benar-benar dikembalikan. */
    fun insertTransaksi(
        id: String, itemId: String, itemNama: String,
        pelangganNama: String, tanggalSewa: String,
        durasiRencana: Int, totalBiaya: Int
    ) {
        getConnection().use { conn ->
            conn.prepareStatement(
                """INSERT INTO transaksi
                   (id, itemId, itemNama, pelangganNama, tanggalSewa, durasiRencana, totalBiaya, selesai)
                   VALUES (?, ?, ?, ?, ?, ?, ?, 0)"""
            ).use { ps ->
                ps.setString(1, id)
                ps.setString(2, itemId)
                ps.setString(3, itemNama)
                ps.setString(4, pelangganNama)
                ps.setString(5, tanggalSewa)
                ps.setInt(6, durasiRencana)
                ps.setInt(7, totalBiaya)
                ps.executeUpdate()
            }
        }
    }

    fun getAllTransaksi(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        getConnection().use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT * FROM transaksi ORDER BY rowid DESC").use { rs ->
                    while (rs.next()) list.add(rs.toTransaksi())
                }
            }
        }
        return list
    }

    fun getTransaksiById(id: String): Map<String, Any>? =
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM transaksi WHERE id = ?").use { ps ->
                ps.setString(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toTransaksi() else null }
            }
        }

    fun selesaikanTransaksi(id: String, tanggalKembali: String, totalBiaya: Int) {
        getConnection().use { conn ->
            conn.prepareStatement(
                "UPDATE transaksi SET tanggalKembali = ?, totalBiaya = ?, selesai = 1 WHERE id = ?"
            ).use { ps ->
                ps.setString(1, tanggalKembali)
                ps.setInt(2, totalBiaya)
                ps.setString(3, id)
                ps.executeUpdate()
            }
        }
    }
}