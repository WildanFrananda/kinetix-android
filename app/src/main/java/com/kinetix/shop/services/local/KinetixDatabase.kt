package com.kinetix.shop.services.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedCategoryEntity::class,
        CachedProductEntity::class,
        CachedProfileEntity::class,
        PendingTransactionStateEntity::class,
        OutboxEntryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class KinetixDatabase : RoomDatabase() {
    abstract fun catalogCacheDao(): CatalogCacheDao

    abstract fun profileCacheDao(): ProfileCacheDao

    abstract fun pendingTransactionStateDao(): PendingTransactionStateDao

    abstract fun outboxDao(): OutboxDao
}
