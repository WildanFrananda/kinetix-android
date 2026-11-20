package com.kinetix.shop.services.outbox

interface OutboxSyncScheduler {
    fun scheduleSync()
}
