package com.sewadulu.service

import com.sewadulu.exception.InputTidakValidException
import com.sewadulu.model.Pelanggan
import com.sewadulu.model.TambahPelangganRequest
import com.sewadulu.repository.PelangganRepository
import com.sewadulu.util.IdGenerator
import com.sewadulu.util.Validasi

/** Logika bisnis pelanggan: validasi nama, email, nomor HP, dan cek email duplikat. */
class PelangganService(private val pelangganRepository: PelangganRepository) {

    @Synchronized
    fun tambah(req: TambahPelangganRequest): Pelanggan {
        val nama = req.nama?.trim()
        if (nama.isNullOrEmpty()) throw InputTidakValidException("Nama pelanggan tidak boleh kosong.")

        val email = req.email?.trim()
        if (email.isNullOrEmpty() || !Validasi.emailValid(email))
            throw InputTidakValidException("Format email tidak valid (contoh: nama@email.com).")

        val noHp = req.noHp?.let { Validasi.bersihkanHp(it) }
        if (noHp.isNullOrEmpty() || !Validasi.hpValid(noHp))
            throw InputTidakValidException("Format nomor HP tidak valid (contoh: 081234567890).")

        if (pelangganRepository.emailSudahTerdaftar(email))
            throw InputTidakValidException("Email '$email' sudah terdaftar.")

        val pelanggan = Pelanggan(IdGenerator.baru("CUST"), nama, email, noHp)
        pelangganRepository.simpan(pelanggan)
        return pelanggan
    }

    fun daftar(): List<Pelanggan> = pelangganRepository.semua()
}
