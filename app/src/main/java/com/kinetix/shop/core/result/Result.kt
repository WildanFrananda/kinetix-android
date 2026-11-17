package com.kinetix.shop.core.result

import com.kinetix.shop.core.failure.Failure

sealed interface Result<out T> {
    fun <R> fold(
        onOk: (T) -> R,
        onErr: (Failure) -> R,
    ): R {
        return when (this) {
            is Ok -> onOk(value)
            is Err -> onErr(failure)
        }
    }
}
