package com.kinetix.shop.services.notification

import com.kinetix.shop.fakes.FakeAnalyticsTracker
import com.kinetix.shop.fakes.FakeCrashReporter
import com.kinetix.shop.fakes.FakeFcmTokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultPushMessageHandlerTest {
    @Test
    fun storesRefreshedTokenAndCapturesDeviceVerificationChallenge() {
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
                "type" to "DEVICE_VERIFICATION",
                "challengeId" to "chal-99",
                "challengeToken" to "tok-secret-99",
            )
        )
        val challenge = handler.latestVerificationChallenge.value
        assertNotNull(challenge)
        assertEquals("chal-99", challenge?.challengeId)
        assertEquals("tok-secret-99", challenge?.challengeToken)

        handler.clearVerificationChallenge()
        assertNull(handler.latestVerificationChallenge.value)
    }
}
