package com.sewadulu.util

import java.util.UUID

/** Pembuat ID unik, mis. ITM-3F9A2C, CUST-81B2D4, TRX-C07E15. */
object IdGenerator {
    fun baru(prefix: String): String =
        prefix + "-" + UUID.randomUUID().toString().substring(0, 6).uppercase()
}
