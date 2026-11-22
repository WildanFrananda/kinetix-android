package com.kinetix.shop.services.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedCategoryEntity::class,
        CachedProductEntity::class,
        CachedProfileEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class KinetixCacheDatabase : RoomDatabase() {
    abstract fun catalogCacheDao(): CatalogCacheDao

    abstract fun profileCacheDao(): ProfileCacheDao
}
