package com.kinetix.shop.fakes

import com.kinetix.shop.core.network.RequestIdGenerator

class FakeRequestIdGenerator(
    private val requestId: String = "00000000-0000-4000-8000-000000000001",
) : RequestIdGenerator {
    override fun newRequestId(): String {
        return requestId
    }
}
