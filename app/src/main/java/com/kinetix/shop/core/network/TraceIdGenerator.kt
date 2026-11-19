package com.kinetix.shop.core.network

interface TraceIdGenerator {
    fun newTraceIdHex(): String

    fun newSpanIdHex(): String
}
