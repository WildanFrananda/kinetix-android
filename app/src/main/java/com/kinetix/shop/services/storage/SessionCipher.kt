package com.kinetix.shop.services.storage

interface SessionCipher {
    fun encrypt(plaintext: ByteArray): ByteArray

    fun decrypt(ciphertext: ByteArray): ByteArray
}
