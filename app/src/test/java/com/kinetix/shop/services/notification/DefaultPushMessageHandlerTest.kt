package com.kinetix.shop.services.notification

import com.kinetix.shop.fakes.FakeAnalyticsTracker
import com.kinetix.shop.fakes.FakeCrashReporter
import com.kinetix.shop.fakes.FakeFcmTokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultPushMessageHandlerTest {
    @Test
    fun storesRefreshedTokenAndCapturesDeviceVerificationChallengeWithoutLoggingNonce() {
        val tokenStore = FakeFcmTokenStore()
        val crashReporter = FakeCrashReporter()
        val analyticsTracker = FakeAnalyticsTracker()
        val handler = DefaultPushMessageHandler(
            fcmTokenStore = tokenStore,
            crashReporter = crashReporter,
            analyticsTracker = analyticsTracker,
        )

        handler.onNewDeviceToken(" fcm-token-123 ")
        assertEquals("fcm-token-123", tokenStore.getToken())
        assertEquals("fcm_token_refreshed", analyticsTracker.events.single().first)

        handler.onDataMessageReceived(
            mapOf(
                "kind" to "DEVICE_VERIFICATION",
                "nonce" to "nonce-secret-99",
                "expires_at" to "2026-11-22T12:00:00Z",
            )
        )
        val challenge = handler.latestVerificationChallenge.value
        assertNotNull(challenge)
        assertEquals("nonce-secret-99", challenge?.nonce)
        assertEquals("2026-11-22T12:00:00Z", challenge?.expiresAt)
        assertFalse(crashReporter.breadcrumbs.any { it.second.contains("nonce-secret-99") })

        handler.clearVerificationChallenge()
        assertNull(handler.latestVerificationChallenge.value)
    }
}
