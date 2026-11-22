package com.kinetix.shop.services.notification

import com.kinetix.shop.models.notification.DeviceVerificationChallenge
import com.kinetix.shop.services.telemetry.AnalyticsTracker
import com.kinetix.shop.services.telemetry.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DefaultPushMessageHandler @Inject constructor(
    private val fcmTokenStore: FcmTokenStore,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
) : PushMessageHandler {
    private val mutableVerificationChallenge = MutableStateFlow<DeviceVerificationChallenge?>(null)
    override val latestVerificationChallenge: StateFlow<DeviceVerificationChallenge?> =
        mutableVerificationChallenge.asStateFlow()

    override fun onNewDeviceToken(
        token: String,
    ) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            return
        }
        fcmTokenStore.saveToken(trimmed)
        crashReporter.recordBreadcrumb(
            category = "fcm",
            message = "Updated FCM registration token",
        )
        analyticsTracker.logEvent(
            name = "fcm_token_refreshed",
        )
    }

    override fun onDataMessageReceived(
        data: Map<String, String>,
    ) {
        val messageKind = data[KEY_KIND].orEmpty().ifEmpty { data[KEY_TYPE].orEmpty() }
        if (messageKind == KIND_DEVICE_VERIFICATION) {
            val nonce = data[KEY_NONCE].orEmpty().trim()
            val expiresAt = data[KEY_EXPIRES_AT].orEmpty().trim()
            if (nonce.isNotEmpty() && expiresAt.isNotEmpty()) {
                mutableVerificationChallenge.value = DeviceVerificationChallenge(
                    nonce = nonce,
                    expiresAt = expiresAt,
                )
                crashReporter.recordBreadcrumb(
                    category = "fcm",
                    message = "Received DEVICE_VERIFICATION challenge expires_at=$expiresAt",
                )
            }
            return
        }

        if (messageKind.isNotEmpty()) {
            analyticsTracker.logEvent(
                name = "push_message_received",
                params = mapOf(KEY_KIND to messageKind),
            )
        }
    }

    override fun clearVerificationChallenge() {
        mutableVerificationChallenge.value = null
    }

    companion object {
        const val KEY_KIND = "kind"
        const val KEY_TYPE = "type"
        const val KIND_DEVICE_VERIFICATION = "DEVICE_VERIFICATION"
        const val KEY_NONCE = "nonce"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
