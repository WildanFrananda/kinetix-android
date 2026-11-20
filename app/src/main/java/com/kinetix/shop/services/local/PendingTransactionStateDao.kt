package com.kinetix.shop.services.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingTransactionStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(
        entity: PendingTransactionStateEntity,
    )

    @Query("SELECT * FROM pending_transaction_states WHERE stateKey = :stateKey LIMIT 1")
    suspend fun findByKey(
        stateKey: String,
    ): PendingTransactionStateEntity?

    @Query("DELETE FROM pending_transaction_states WHERE stateKey = :stateKey")
    suspend fun deleteByKey(
        stateKey: String,
    )

    @Query("DELETE FROM pending_transaction_states WHERE principalId = :principalId")
    suspend fun deleteByPrincipalId(
        principalId: String,
    )
}
