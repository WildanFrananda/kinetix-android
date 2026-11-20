package com.kinetix.shop.services.local

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.TopUpReference
import com.kinetix.shop.models.transaction.PendingTransactionOperationType
import com.kinetix.shop.models.transaction.PendingTransactionState

interface PendingTransactionStore {
    suspend fun getOrCreateKey(
        principalId: PrincipalId,
        operationType: PendingTransactionOperationType,
        payloadFingerprint: String,
    ): PendingTransactionState

    suspend fun attachReferenceNumber(
        stateKey: String,
        referenceNumber: TopUpReference,
    )

    suspend fun find(
        principalId: PrincipalId,
        operationType: PendingTransactionOperationType,
    ): PendingTransactionState?

    suspend fun clear(
        principalId: PrincipalId,
        operationType: PendingTransactionOperationType,
    )

    suspend fun clearAllForPrincipal(
        principalId: PrincipalId,
    )
}
