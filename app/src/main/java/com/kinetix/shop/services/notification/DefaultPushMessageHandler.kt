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
        val messageType = data[KEY_TYPE].orEmpty()
        if (messageType == TYPE_DEVICE_VERIFICATION) {
            val challengeId = data[KEY_CHALLENGE_ID].orEmpty().trim()
            val challengeToken = data[KEY_CHALLENGE_TOKEN].orEmpty().trim()
            if (challengeId.isNotEmpty() && challengeToken.isNotEmpty()) {
                mutableVerificationChallenge.value = DeviceVerificationChallenge(
                    challengeId = challengeId,
                    challengeToken = challengeToken,
                )
                crashReporter.recordBreadcrumb(
                    category = "fcm",
                    message = "Received DEVICE_VERIFICATION challenge $challengeId",
                )
            }
            return
        }

        if (messageType.isNotEmpty()) {
            analyticsTracker.logEvent(
                name = "push_message_received",
                params = mapOf(KEY_TYPE to messageType),
            )
        }
    }

    override fun clearVerificationChallenge() {
        mutableVerificationChallenge.value = null
    }

    companion object {
        const val KEY_TYPE = "type"
        const val TYPE_DEVICE_VERIFICATION = "DEVICE_VERIFICATION"
        const val KEY_CHALLENGE_ID = "challengeId"
        const val KEY_CHALLENGE_TOKEN = "challengeToken"
    }
}
