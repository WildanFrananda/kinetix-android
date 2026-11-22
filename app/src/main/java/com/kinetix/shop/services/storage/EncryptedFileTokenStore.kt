package com.kinetix.shop.services.storage

import com.kinetix.shop.core.failure.StorageFailure
import com.kinetix.shop.core.failure.StorageFailureReason
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.dto.auth.StoredSessionPayloadDto
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class EncryptedFileTokenStore(
    private val storageDirectory: File,
    private val cipher: SessionCipher,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) : TokenStore {
    private val mutex = Mutex()
    private val targetFile = File(
        storageDirectory,
        "kinetix_session.enc",
    )
    private val tempFile = File(
        storageDirectory,
        "kinetix_session.enc.tmp",
    )

    override suspend fun read(): Result<StoredSession?> {
        return mutex.withLock {
            withContext(ioDispatcher) {
                try {
                    if (!targetFile.exists()) {
                        return@withContext Ok(null)
                    }

                    val encryptedBytes = targetFile.readBytes()
                    val decryptedBytes = cipher.decrypt(encryptedBytes)
                    val payload = json.decodeFromString(
                        StoredSessionPayloadDto.serializer(),
                        decryptedBytes.decodeToString(),
                    )

                    Ok(
                        StoredSession(
                            accessToken = payload.accessToken,
                            refreshToken = payload.refreshToken,
                            expiresAtEpochSeconds = payload.expiresAtEpochSeconds,
                            accountId = AccountId(payload.accountId),
                            principalId = PrincipalId(payload.principalId),
                            email = payload.email,
                            role = payload.role,
                        )
                    )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    Err(StorageFailure(StorageFailureReason.READ_FAILED))
                }
            }
        }
    }

    override suspend fun write(session: StoredSession): Result<UnitSentinel> {
        return mutex.withLock {
            withContext(ioDispatcher) {
                try {
                    if (!storageDirectory.exists() && !storageDirectory.mkdirs()) {
                        return@withContext Err(StorageFailure(StorageFailureReason.DIRECTORY_CREATION_FAILED))
                    }

                    val payload = StoredSessionPayloadDto(
                        accessToken = session.accessToken,
                        refreshToken = session.refreshToken,
                        expiresAtEpochSeconds = session.expiresAtEpochSeconds,
                        accountId = session.accountId.value,
                        principalId = session.principalId.value,
                        email = session.email,
                        role = session.role,
                    )
                    val serialized = json.encodeToString(
                        StoredSessionPayloadDto.serializer(),
                        payload,
                    )
                    val encryptedBytes = cipher.encrypt(serialized.encodeToByteArray())
                    tempFile.writeBytes(encryptedBytes)
                    Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )

                    Ok(UnitSentinel)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    Err(StorageFailure(StorageFailureReason.WRITE_FAILED))
                }
            }
        }
    }

    override suspend fun clear(): Result<UnitSentinel> {
        return mutex.withLock {
            withContext(ioDispatcher) {
                try {
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }

                    if (targetFile.exists() && !targetFile.delete()) {
                        return@withContext Err(StorageFailure(StorageFailureReason.CLEAR_FAILED))
                    }

                    Ok(UnitSentinel)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    Err(StorageFailure(StorageFailureReason.CLEAR_FAILED))
                }
            }
        }
    }
}
