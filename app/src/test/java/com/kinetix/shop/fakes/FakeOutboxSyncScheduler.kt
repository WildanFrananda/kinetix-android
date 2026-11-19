package com.kinetix.shop.fakes

import com.kinetix.shop.services.outbox.OutboxSyncScheduler

class FakeOutboxSyncScheduler : OutboxSyncScheduler {
    var scheduleCount: Int = 0
        private set

    override fun scheduleSync() {
        scheduleCount += 1
    }
}
