package com.kinetix.shop.services.config

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kinetix.shop.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseFeatureFlagProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : FeatureFlagProvider {
    private val defaultsRequested = AtomicBoolean(false)
    private val defaultsReady = AtomicBoolean(false)

    override fun isForceUpdateRequired(
        currentVersionCode: Int,
    ): Boolean {
        val remoteConfig = readyRemoteConfigOrNull() ?: return false
        val forceUpdateFlag = remoteConfig.getBoolean(KEY_FORCE_UPDATE_ENABLED)
        val minimumVersionCode = remoteConfig.getLong(KEY_MINIMUM_SUPPORTED_VERSION_CODE).coerceAtLeast(1L)
        return forceUpdateFlag || currentVersionCode.toLong() < minimumVersionCode
    }

    override fun isCheckoutEnabled(): Boolean {
        val remoteConfig = readyRemoteConfigOrNull() ?: return true
        return remoteConfig.getBoolean(KEY_CHECKOUT_ENABLED)
    }

    override fun isWalletTopUpEnabled(): Boolean {
        val remoteConfig = readyRemoteConfigOrNull() ?: return true
        return remoteConfig.getBoolean(KEY_WALLET_TOPUP_ENABLED)
    }

    override fun isLiveTrackingEnabled(): Boolean {
        val remoteConfig = readyRemoteConfigOrNull() ?: return true
        return remoteConfig.getBoolean(KEY_LIVE_TRACKING_ENABLED)
    }

    override fun isOutboxSyncEnabled(): Boolean {
        val remoteConfig = readyRemoteConfigOrNull() ?: return true
        return remoteConfig.getBoolean(KEY_OUTBOX_SYNC_ENABLED)
    }

    override suspend fun refreshFlags(): Boolean {
        val remoteConfig = remoteConfigInstanceOrNull() ?: return false
        return try {
            defaultsRequested.set(true)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).await()
            defaultsReady.set(true)
            remoteConfig.fetchAndActivate().await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
    }

    private fun readyRemoteConfigOrNull(): FirebaseRemoteConfig? {
        val instance = remoteConfigInstanceOrNull() ?: return null
        if (!defaultsReady.get()) {
            return null
        }
        return instance
    }

    private fun remoteConfigInstanceOrNull(): FirebaseRemoteConfig? {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return null
        }
        val instance = FirebaseRemoteConfig.getInstance()
        if (defaultsRequested.compareAndSet(false, true)) {
            instance.setDefaultsAsync(R.xml.remote_config_defaults)
                .addOnSuccessListener {
                    defaultsReady.set(true)
                }
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
