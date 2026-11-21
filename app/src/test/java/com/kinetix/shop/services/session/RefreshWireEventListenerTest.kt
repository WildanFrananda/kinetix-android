package com.kinetix.shop.services.session

import java.net.InetSocketAddress
import java.net.Proxy
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshWireEventListenerTest {
    private val dummyCall = OkHttpClient().newCall(
        Request.Builder().url("https://api.kinetix.invalid/api/v1/auth/refresh").build()
    )

    @Test
    fun reportsLeftDeviceTrueWhenNoEventsWereDelivered() {
        val listener = RefreshWireEventListener()

        assertTrue(listener.didLeaveDevice())
    }

    @Test
    fun reportsLeftDeviceFalseWhenFailedBeforeRequestHeadersStart() {
        val listener = RefreshWireEventListener()
        listener.callStart(dummyCall)
        listener.dnsStart(dummyCall, "api.kinetix.invalid")
        listener.connectStart(
            dummyCall,
            InetSocketAddress.createUnresolved("api.kinetix.invalid", 443),
            Proxy.NO_PROXY,
        )
        listener.secureConnectStart(dummyCall)

        assertFalse(listener.didLeaveDevice())
    }

    @Test
    fun reportsLeftDeviceTrueOnceRequestHeadersStartOrLaterIsObserved() {
        val headersListener = RefreshWireEventListener()
        headersListener.callStart(dummyCall)
        headersListener.requestHeadersStart(dummyCall)
        assertTrue(headersListener.didLeaveDevice())

        val bodyListener = RefreshWireEventListener()
        bodyListener.callStart(dummyCall)
        bodyListener.requestBodyStart(dummyCall)
        assertTrue(bodyListener.didLeaveDevice())
    }
}
