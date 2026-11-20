package com.kinetix.shop.fakes

import com.kinetix.shop.services.config.FeatureFlagProvider

class FakeFeatureFlagProvider(
    var forceUpdateRequired: Boolean = false,
    var checkoutEnabled: Boolean = true,
    var walletTopUpEnabled: Boolean = true,
    var liveTrackingEnabled: Boolean = true,
    var outboxSyncEnabled: Boolean = true,
) : FeatureFlagProvider {
    override fun isForceUpdateRequired(
        currentVersionCode: Int,
    ): Boolean {
        return forceUpdateRequired
    }

    override fun isCheckoutEnabled(): Boolean {
        return checkoutEnabled
    }

    override fun isWalletTopUpEnabled(): Boolean {
        return walletTopUpEnabled
    }

    override fun isLiveTrackingEnabled(): Boolean {
        return liveTrackingEnabled
    }

    override fun isOutboxSyncEnabled(): Boolean {
        return outboxSyncEnabled
    }

    override suspend fun refreshFlags(): Boolean {
        return true
    }
}
