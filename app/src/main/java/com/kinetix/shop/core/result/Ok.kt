package com.kinetix.shop.core.result

data class Ok<out T>(val value: T) : Result<T>
