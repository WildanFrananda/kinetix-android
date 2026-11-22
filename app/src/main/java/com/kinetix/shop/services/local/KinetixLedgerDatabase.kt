package com.kinetix.shop.services.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        OutboxEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KinetixLedgerDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
}
