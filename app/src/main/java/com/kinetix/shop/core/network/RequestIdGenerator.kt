package com.kinetix.shop.core.network

interface RequestIdGenerator {
    fun newRequestId(): String
}
