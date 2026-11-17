package com.kinetix.shop.fakes

import com.kinetix.shop.core.time.Clock

class FakeClock(
    var currentEpochSeconds: Long = 1_700_000_000L,
) : Clock {
    override fun nowEpochSeconds(): Long {
        return currentEpochSeconds
    }

    override fun nowEpochMillis(): Long {
        return currentEpochSeconds * 1_000L
    }
}
