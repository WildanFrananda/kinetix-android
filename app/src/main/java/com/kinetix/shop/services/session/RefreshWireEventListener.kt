package com.kinetix.shop.services.session

import java.net.InetSocketAddress
import java.net.Proxy
import okhttp3.Call
import okhttp3.EventListener

class RefreshWireEventListener : EventListener() {
    @Volatile
    private var sawAnyEvent: Boolean = false

    @Volatile
    private var sawRequestHeadersStart: Boolean = false

    override fun callStart(
        call: Call,
    ) {
        sawAnyEvent = true
    }

    override fun dnsStart(
        call: Call,
        domainName: String,
    ) {
        sawAnyEvent = true
    }

    override fun connectStart(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
    ) {
        sawAnyEvent = true
    }

    override fun secureConnectStart(
        call: Call,
    ) {
        sawAnyEvent = true
    }

    override fun requestHeadersStart(
        call: Call,
    ) {
        sawAnyEvent = true
        sawRequestHeadersStart = true
    }

    override fun requestBodyStart(
        call: Call,
    ) {
        sawAnyEvent = true
        sawRequestHeadersStart = true
    }

    fun didLeaveDevice(): Boolean {
        return !sawAnyEvent || sawRequestHeadersStart
    }
}
