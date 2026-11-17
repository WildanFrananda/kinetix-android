package com.kinetix.shop.core.time

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun nowEpochSeconds(): Long {
        return System.currentTimeMillis() / 1_000L
    }

    override fun nowEpochMillis(): Long {
        return System.currentTimeMillis()
    }
}
