package com.kinetix.shop.services.storage

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.models.auth.StoredSession

interface TokenStore {
    suspend fun read(): Result<StoredSession?>

    suspend fun write(session: StoredSession): Result<UnitSentinel>

    suspend fun clear(): Result<UnitSentinel>
}
