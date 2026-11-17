package com.kinetix.shop.services.session

interface TokenRefreshTransport {
    suspend fun refresh(refreshToken: String): RefreshTransportOutcome
}
