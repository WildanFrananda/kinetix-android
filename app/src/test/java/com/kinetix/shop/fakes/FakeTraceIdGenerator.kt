package com.kinetix.shop.fakes

import com.kinetix.shop.core.network.TraceIdGenerator

class FakeTraceIdGenerator(
    private val traceId: String = "4bf92f3577b34da6a3ce929d0e0e4736",
    private val spanId: String = "00f067aa0ba902b7",
) : TraceIdGenerator {
    override fun newTraceIdHex(): String {
        return traceId
    }

    override fun newSpanIdHex(): String {
        return spanId
    }
}
