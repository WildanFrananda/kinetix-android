package com.kinetix.shop.services.config

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kinetix.shop.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseFeatureFlagProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : FeatureFlagProvider {
    private val defaultsApplied = AtomicBoolean(false)

    override fun isForceUpdateRequired(
        currentVersionCode: Int,
    ): Boolean {
        val remoteConfig = remoteConfigOrNull() ?: return false
        val forceUpdateFlag = remoteConfig.getBoolean(KEY_FORCE_UPDATE_ENABLED)
        val minimumVersionCode = remoteConfig.getLong(KEY_MINIMUM_SUPPORTED_VERSION_CODE).coerceAtLeast(1L)
        return forceUpdateFlag || currentVersionCode.toLong() < minimumVersionCode
    }

    override fun isCheckoutEnabled(): Boolean {
        val remoteConfig = remoteConfigOrNull() ?: return true
        return remoteConfig.getBoolean(KEY_CHECKOUT_ENABLED)
    }

    override fun isWalletTopUpEnabled(): Boolean {
        val remoteConfig = remoteConfigOrNull() ?: return true
        return remoteConfig.getBoolean(KEY_WALLET_TOPUP_ENABLED)
    }

    override fun isLiveTrackingEnabled(): Boolean {
        val remoteConfig = remoteConfigOrNull() ?: return true
        return remoteConfig.getBoolean(KEY_LIVE_TRACKING_ENABLED)
    }

    override fun isOutboxSyncEnabled(): Boolean {
        val remoteConfig = remoteConfigOrNull() ?: return true
        return remoteConfig.getBoolean(KEY_OUTBOX_SYNC_ENABLED)
    }

    override suspend fun refreshFlags(): Boolean {
        val remoteConfig = remoteConfigOrNull() ?: return false
        return try {
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).await()
            defaultsApplied.set(true)
            remoteConfig.fetchAndActivate().await()
        } catch (_: Exception) {
            false
        }
    }

    private fun remoteConfigOrNull(): FirebaseRemoteConfig? {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return null
        }
        val instance = FirebaseRemoteConfig.getInstance()
        if (defaultsApplied.compareAndSet(false, true)) {
            instance.setDefaultsAsync(R.xml.remote_config_defaults)
        }
        return instance
    }

    companion object {
        const val KEY_MINIMUM_SUPPORTED_VERSION_CODE = "minimum_supported_version_code"
        const val KEY_FORCE_UPDATE_ENABLED = "force_update_enabled"
        const val KEY_CHECKOUT_ENABLED = "checkout_enabled"
        const val KEY_WALLET_TOPUP_ENABLED = "wallet_topup_enabled"
        const val KEY_LIVE_TRACKING_ENABLED = "live_tracking_enabled"
        const val KEY_OUTBOX_SYNC_ENABLED = "outbox_sync_enabled"
    }
}
