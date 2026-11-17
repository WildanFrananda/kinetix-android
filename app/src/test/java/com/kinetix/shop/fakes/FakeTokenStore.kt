package com.kinetix.shop.fakes

import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.services.storage.TokenStore

class FakeTokenStore(
    var current: StoredSession? = null,
) : TokenStore {
    var clearCount: Int = 0

    override suspend fun read(): Result<StoredSession?> {
        return Ok(current)
    }

    override suspend fun write(session: StoredSession): Result<UnitSentinel> {
        current = session
        return Ok(UnitSentinel)
    }

    override suspend fun clear(): Result<UnitSentinel> {
        current = null
        clearCount += 1
        return Ok(UnitSentinel)
    }
}
