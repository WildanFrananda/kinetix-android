package com.kinetix.shop.services.config

import android.content.Context
import com.kinetix.shop.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.xmlpull.v1.XmlPullParser

@Singleton
class LocalFeatureFlagProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : FeatureFlagProvider {
    private val defaults: Map<String, String> by lazy {
        loadAndValidateBundledDefaults()
    }

    override fun isForceUpdateRequired(
        currentVersionCode: Int,
    ): Boolean {
        val forceUpdateFlag = requireBoolean(FirebaseFeatureFlagProvider.KEY_FORCE_UPDATE_ENABLED)
        val minimumVersionCode = requirePositiveLong(
            FirebaseFeatureFlagProvider.KEY_MINIMUM_SUPPORTED_VERSION_CODE,
        )
        return forceUpdateFlag || currentVersionCode.toLong() < minimumVersionCode
    }

    override fun isCheckoutEnabled(): Boolean {
        return requireBoolean(FirebaseFeatureFlagProvider.KEY_CHECKOUT_ENABLED)
    }

    override fun isWalletTopUpEnabled(): Boolean {
        return requireBoolean(FirebaseFeatureFlagProvider.KEY_WALLET_TOPUP_ENABLED)
    }

    override fun isLiveTrackingEnabled(): Boolean {
        return requireBoolean(FirebaseFeatureFlagProvider.KEY_LIVE_TRACKING_ENABLED)
    }

    override fun isOutboxSyncEnabled(): Boolean {
        return requireBoolean(FirebaseFeatureFlagProvider.KEY_OUTBOX_SYNC_ENABLED)
    }

    override suspend fun refreshFlags(): Boolean {
        return false
    }

    private fun requireBoolean(
        key: String,
    ): Boolean {
        val raw = defaults[key]
            ?: throw IllegalStateException("Missing bundled feature flag: $key")
        return raw.toBooleanStrictOrNull()
            ?: throw IllegalStateException("Invalid boolean bundled feature flag for $key: $raw")
    }

    private fun requirePositiveLong(
        key: String,
    ): Long {
        val raw = defaults[key]
            ?: throw IllegalStateException("Missing bundled feature flag: $key")
        val value = raw.toLongOrNull()
            ?: throw IllegalStateException("Invalid integer bundled feature flag for $key: $raw")
        if (value < 1L) {
            throw IllegalStateException("Bundled feature flag $key must be >= 1, got $value")
        }
        return value
    }

    private fun loadAndValidateBundledDefaults(): Map<String, String> {
        val parsed = linkedMapOf<String, String>()
        val parser = context.resources.getXml(R.xml.remote_config_defaults)
        parser.use { parser ->
            var currentKey: String? = null
            var currentTag: String? = null
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "key" -> currentKey = text
                                "value" -> {
                                    val key = currentKey
                                    if (key != null) {
                                        parsed[key] = text
                                        currentKey = null
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        }

        val requiredBooleanKeys = listOf(
            FirebaseFeatureFlagProvider.KEY_FORCE_UPDATE_ENABLED,
            FirebaseFeatureFlagProvider.KEY_CHECKOUT_ENABLED,
            FirebaseFeatureFlagProvider.KEY_WALLET_TOPUP_ENABLED,
            FirebaseFeatureFlagProvider.KEY_LIVE_TRACKING_ENABLED,
            FirebaseFeatureFlagProvider.KEY_OUTBOX_SYNC_ENABLED,
        )
        for (key in requiredBooleanKeys) {
            val raw = parsed[key]
                ?: throw IllegalStateException("Missing required bundled feature flag: $key")
            if (raw.toBooleanStrictOrNull() == null) {
                throw IllegalStateException("Invalid boolean value for bundled feature flag $key: $raw")
            }
        }

        val minVersionRaw = parsed[FirebaseFeatureFlagProvider.KEY_MINIMUM_SUPPORTED_VERSION_CODE]
            ?: throw IllegalStateException(
                "Missing required bundled feature flag: ${FirebaseFeatureFlagProvider.KEY_MINIMUM_SUPPORTED_VERSION_CODE}"
            )
        val minVersion = minVersionRaw.toLongOrNull()
        if (minVersion == null || minVersion < 1L) {
            throw IllegalStateException(
                "Invalid minimum_supported_version_code in bundled feature flags: $minVersionRaw"
            )
        }

        return parsed
    }
}
