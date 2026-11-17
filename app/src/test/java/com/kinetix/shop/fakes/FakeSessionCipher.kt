package com.kinetix.shop.fakes

import com.kinetix.shop.services.storage.SessionCipher
import java.io.IOException

class FakeSessionCipher : SessionCipher {
    var shouldFailDecrypt: Boolean = false

    override fun encrypt(plaintext: ByteArray): ByteArray {
        return plaintext.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        if (shouldFailDecrypt) {
            throw IOException("Simulated cipher failure")
        }
        return ciphertext.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
    }
}
