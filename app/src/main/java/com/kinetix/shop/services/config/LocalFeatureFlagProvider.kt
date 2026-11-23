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
        loadBundledDefaults()
    }

    override fun isForceUpdateRequired(
        currentVersionCode: Int,
    ): Boolean {
        val forceUpdateFlag = defaults[FirebaseFeatureFlagProvider.KEY_FORCE_UPDATE_ENABLED]
            ?.toBooleanStrictOrNull() ?: false
        val minimumVersionCode = defaults[FirebaseFeatureFlagProvider.KEY_MINIMUM_SUPPORTED_VERSION_CODE]
            ?.toLongOrNull()
            ?.coerceAtLeast(1L) ?: 1L
        return forceUpdateFlag || currentVersionCode.toLong() < minimumVersionCode
    }

    override fun isCheckoutEnabled(): Boolean {
        return defaults[FirebaseFeatureFlagProvider.KEY_CHECKOUT_ENABLED]
            ?.toBooleanStrictOrNull() ?: true
    }

    override fun isWalletTopUpEnabled(): Boolean {
        return defaults[FirebaseFeatureFlagProvider.KEY_WALLET_TOPUP_ENABLED]
            ?.toBooleanStrictOrNull() ?: true
    }

    override fun isLiveTrackingEnabled(): Boolean {
        return defaults[FirebaseFeatureFlagProvider.KEY_LIVE_TRACKING_ENABLED]
            ?.toBooleanStrictOrNull() ?: true
    }

    override fun isOutboxSyncEnabled(): Boolean {
        return defaults[FirebaseFeatureFlagProvider.KEY_OUTBOX_SYNC_ENABLED]
            ?.toBooleanStrictOrNull() ?: true
    }

    override suspend fun refreshFlags(): Boolean {
        return false
    }

    private fun loadBundledDefaults(): Map<String, String> {
        val parsed = linkedMapOf<String, String>()
        val parser = try {
            context.resources.getXml(R.xml.remote_config_defaults)
        } catch (_: Exception) {
            return emptyMap()
        }
        try {
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
        } catch (_: Exception) {
            return parsed
        } finally {
            parser.close()
        }
        return parsed
    }
}
