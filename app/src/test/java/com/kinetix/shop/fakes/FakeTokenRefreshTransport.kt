package com.kinetix.shop.fakes

import com.kinetix.shop.services.session.RefreshTransportOutcome
import com.kinetix.shop.services.session.TokenRefreshTransport
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FakeTokenRefreshTransport(
    var nextOutcome: RefreshTransportOutcome,
    var simulatedDelayMillis: Long = 0L,
) : TokenRefreshTransport {
    var callCount: Int = 0
    val seenRefreshTokens = mutableListOf<String>()

    override suspend fun refresh(refreshToken: String): RefreshTransportOutcome {
        callCount += 1
        seenRefreshTokens.add(refreshToken)
        if (simulatedDelayMillis > 0L) {
            delay(simulatedDelayMillis.milliseconds)
        }
        return nextOutcome
    }
}
