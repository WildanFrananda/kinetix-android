package com.kinetix.shop.core.time

interface Clock {
    fun nowEpochSeconds(): Long

    fun nowEpochMillis(): Long
}
