package com.kinetix.shop.services.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(
        profile: CachedProfileEntity,
    )

    @Query("SELECT * FROM cached_profiles WHERE accountId = :accountId LIMIT 1")
    suspend fun findByAccountId(
        accountId: Long,
    ): CachedProfileEntity?

    @Query("DELETE FROM cached_profiles WHERE accountId = :accountId")
    suspend fun deleteByAccountId(
        accountId: Long,
    )
}
