package com.kinetix.shop.services.session

interface RefreshWireExecutor {
    suspend fun postRefresh(
        refreshToken: String,
    ): RefreshWireOutcome
}
