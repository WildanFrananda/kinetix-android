package com.kinetix.shop.services.outbox

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kinetix.shop.models.outbox.OutboxFlushOutcome
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OutboxFlushWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val outboxProcessor: OutboxProcessor,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return when (outboxProcessor.flushPendingEntries()) {
            OutboxFlushOutcome.COMPLETED -> Result.success()
            OutboxFlushOutcome.RETRY_LATER -> Result.retry()
        }
    }
}
