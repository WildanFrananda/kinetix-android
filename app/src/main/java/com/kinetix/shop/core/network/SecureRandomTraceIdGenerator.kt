package com.kinetix.shop.core.network

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureRandomTraceIdGenerator @Inject constructor() : TraceIdGenerator {
    private val secureRandom = SecureRandom()

    override fun newTraceIdHex(): String {
        return generateNonZeroHex(16)
    }

    override fun newSpanIdHex(): String {
        return generateNonZeroHex(8)
    }

    private fun generateNonZeroHex(
        byteLength: Int,
    ): String {
        val bytes = ByteArray(byteLength)
        do {
            secureRandom.nextBytes(bytes)
        } while (bytes.all { it == 0.toByte() })

        val builder = StringBuilder(byteLength * 2)
        for (byte in bytes) {
            val unsigned = byte.toInt() and 0xFF
            builder.append(HEX_CHARS[unsigned ushr 4])
            builder.append(HEX_CHARS[unsigned and 0x0F])
        }
        return builder.toString()
    }

    companion object {
        private const val HEX_CHARS = "0123456789abcdef"
    }
}
