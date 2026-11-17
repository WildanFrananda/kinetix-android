package com.kinetix.shop.core.failure

data class SignInDelayedFailure(val retryAfterSeconds: Int) : Failure
