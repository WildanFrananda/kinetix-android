package com.kinetix.shop.models.transaction

import com.kinetix.shop.models.ids.IdempotencyKey
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.TopUpReference

data class PendingTransactionState(
    val stateKey: String,
    val principalId: PrincipalId,
    val operationType: PendingTransactionOperationType,
    val idempotencyKey: IdempotencyKey,
    val referenceNumber: TopUpReference?,
    val payloadFingerprint: String,
    val createdAtEpochSeconds: Long,
    val updatedAtEpochSeconds: Long,
)
