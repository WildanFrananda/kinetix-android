package com.kinetix.shop.services.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyStoreSessionCipher @Inject constructor() : SessionCipher {
    private val keyStoreType = "AndroidKeyStore"
    private val keyAlias = "kinetix_buyer_session_key_v1"
    private val transformation = "AES/GCM/NoPadding"
    private val ivLengthBytes = 12
    private val tagLengthBits = 128

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey(),
        )
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext)
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(
            iv,
            0,
            combined,
            0,
            iv.size,
        )
        System.arraycopy(
            encrypted,
            0,
            combined,
            iv.size,
            encrypted.size,
        )

        return combined
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        require(ciphertext.size > ivLengthBytes) {
            "Ciphertext is shorter than GCM IV"
        }
        val iv = ciphertext.copyOfRange(
            0,
            ivLengthBytes,
        )
        val payload = ciphertext.copyOfRange(
            ivLengthBytes,
            ciphertext.size,
        )
        val cipher = Cipher.getInstance(transformation)
        val spec = GCMParameterSpec(
            tagLengthBits,
            iv,
        )
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            spec,
        )

        return cipher.doFinal(payload)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreType)
        keyStore.load(null)
        val existingEntry = keyStore.getEntry(
            keyAlias,
            null,
        ) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            keyStoreType,
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)

        return keyGenerator.generateKey()
    }
}
