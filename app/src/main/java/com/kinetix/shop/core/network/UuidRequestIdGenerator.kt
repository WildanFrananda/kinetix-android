package com.kinetix.shop.core.network

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UuidRequestIdGenerator @Inject constructor() : RequestIdGenerator {
    override fun newRequestId(): String {
        return UUID.randomUUID().toString()
    }
}
