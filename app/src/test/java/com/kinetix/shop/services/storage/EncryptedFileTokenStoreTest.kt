package com.kinetix.shop.services.storage

import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeSessionCipher
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedFileTokenStoreTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun roundTripsSessionThroughCipherAndClearsCleanly() = runTest {
        val tempDir = Files.createTempDirectory("kinetix_token_test").toFile()
        val cipher = FakeSessionCipher()
        val store = EncryptedFileTokenStore(
            storageDirectory = tempDir,
            cipher = cipher,
            json = json,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        val initialRead = store.read()
        assertTrue(initialRead is Ok)
        assertNull((initialRead as Ok).value)

        val session = StoredSession(
            accessToken = "acc-123",
            refreshToken = "ref-456",
            expiresAtEpochSeconds = 1_800_000_000L,
            accountId = AccountId(42L),
            principalId = PrincipalId("usr-uuid-42"),
            email = "buyer@kinetix.test",
            role = "seller",
        )

        val writeResult = store.write(session)
        assertTrue(writeResult is Ok)
        assertTrue(File(tempDir, "kinetix_session.enc").exists())

        val secondRead = store.read()
        assertTrue(secondRead is Ok)
        assertEquals(session, (secondRead as Ok).value)

        store.clear()
        assertFalse(File(tempDir, "kinetix_session.enc").exists())
        tempDir.deleteRecursively()
    }

    @Test
    fun returnsErrWhenCipherFailsToDecrypt() = runTest {
        val tempDir = Files.createTempDirectory("kinetix_corrupt_test").toFile()
        File(tempDir, "kinetix_session.enc").writeBytes(byteArrayOf(1, 2, 3, 4))
        val cipher = FakeSessionCipher().apply {
            shouldFailDecrypt = true
        }
        val store = EncryptedFileTokenStore(
            storageDirectory = tempDir,
            cipher = cipher,
            json = json,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        val readResult = store.read()
        assertTrue(readResult is Err)
        tempDir.deleteRecursively()
    }
}
