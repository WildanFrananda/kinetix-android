package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.core.result.UnitSentinel
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.services.storage.TokenStore

class FakeTokenStore(
    var current: StoredSession? = null,
    var readFailure: Failure? = null,
    var writeFailure: Failure? = null,
) : TokenStore {
    var clearCount: Int = 0

    override suspend fun read(): Result<StoredSession?> {
        val failure = readFailure
        if (failure != null) {
            return Err(failure)
        }

        return Ok(current)
    }

    override suspend fun write(session: StoredSession): Result<UnitSentinel> {
        val failure = writeFailure
        if (failure != null) {
            return Err(failure)
        }

        current = session
        return Ok(UnitSentinel)
    }

    override suspend fun clear(): Result<UnitSentinel> {
        current = null
        clearCount += 1
        return Ok(UnitSentinel)
    }
}
