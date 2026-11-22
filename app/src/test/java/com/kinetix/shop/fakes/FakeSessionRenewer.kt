package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.services.session.SessionRenewer

class FakeSessionRenewer(
    var nextResult: Result<StoredSession> = Err(
        UnauthorizedFailure("SESSION_EXPIRED")
    ),
    private val onRenew: (() -> Unit)? = null,
) : SessionRenewer {
    var renewCalls: Int = 0

    override suspend fun renew(failedAccessToken: String?): Result<StoredSession> {
        renewCalls += 1
        onRenew?.invoke()
        return nextResult
    }
}
