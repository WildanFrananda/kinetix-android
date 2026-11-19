package com.kinetix.shop.services.config

interface FeatureFlagProvider {
    fun isForceUpdateRequired(
        currentVersionCode: Int,
    ): Boolean

    fun isCheckoutEnabled(): Boolean

    fun isWalletTopUpEnabled(): Boolean

    fun isLiveTrackingEnabled(): Boolean

    fun isOutboxSyncEnabled(): Boolean

    suspend fun refreshFlags(): Boolean
}
