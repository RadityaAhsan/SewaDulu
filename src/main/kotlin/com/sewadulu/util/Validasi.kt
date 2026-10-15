package com.sewadulu.util

/** Aturan validasi yang dipakai bersama oleh semua service (satu tempat, tidak duplikat). */
object Validasi {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    // Format nomor HP Indonesia: 08xx / 628xx / +628xx
    private val hpRegex = Regex("^(\\+62|62|0)8[0-9]{8,11}$")

    fun emailValid(email: String): Boolean = emailRegex.matches(email)

    fun bersihkanHp(hp: String): String = hp.trim().replace(" ", "").replace("-", "")

    fun hpValid(hp: String): Boolean = hpRegex.matches(hp)

    /**
     * Ambil angka dari isian lalu tambahkan satuan otomatis.
     * "24", "24 MP", "24mp" -> "24 MP". Mengembalikan null jika isian bukan angka.
     */
    fun formatDenganSatuan(input: String, satuan: String): String? {
        val pola = Regex("^(\\d+(?:[.,]\\d+)?)\\s*(?:$satuan)?$", RegexOption.IGNORE_CASE)
        val cocok = pola.matchEntire(input.trim()) ?: return null
        return "${cocok.groupValues[1]} $satuan"
    }
}
