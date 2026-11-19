package com.kinetix.shop.core.failure

data class ClientValidationFailure(
    val reason: ClientValidationReason,
) : Failure
