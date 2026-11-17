package com.kinetix.shop.core.time

import com.kinetix.shop.core.config.NetworkPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DelayCountdownTimer(
    private val scope: CoroutineScope,
) {
    private var activeJob: Job? = null

    fun start(
        seconds: Int,
        onTick: (Int) -> Unit,
        onFinished: () -> Unit,
    ) {
        activeJob?.cancel()
        activeJob = scope.launch {
            var remaining = seconds.coerceAtLeast(1)
            while (remaining > 0) {
                onTick(remaining)
                delay(NetworkPolicy.ONE_SECOND_MILLIS.milliseconds)
                remaining -= 1
            }
            onFinished()
        }
    }

    fun cancel() {
        activeJob?.cancel()
        activeJob = null
    }
}
