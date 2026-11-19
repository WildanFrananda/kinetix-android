package com.kinetix.shop.core.time

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class DelayCountdownTimerTest {
    @Test
    fun ticksEverySecondUntilFinishedAndCanBeCancelled() = runTest {
        val timer = DelayCountdownTimer(this)
        val ticks = mutableListOf<Int>()
        var finished = false

        timer.start(
            seconds = 3,
            onTick = { ticks.add(it) },
            onFinished = { finished = true },
        )

        advanceTimeBy(3_500L.milliseconds)
        assertEquals(listOf(3, 2, 1), ticks)
        assertTrue(finished)
    }
}
