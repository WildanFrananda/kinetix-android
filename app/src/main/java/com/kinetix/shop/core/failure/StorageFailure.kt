package com.kinetix.shop.core.failure

data class StorageFailure(
    val reason: StorageFailureReason = StorageFailureReason.READ_FAILED,
) : Failure
