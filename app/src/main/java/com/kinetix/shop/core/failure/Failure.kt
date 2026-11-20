package com.kinetix.shop.core.failure

sealed interface Failure {
    val requestId: String?
        get() = null
}
