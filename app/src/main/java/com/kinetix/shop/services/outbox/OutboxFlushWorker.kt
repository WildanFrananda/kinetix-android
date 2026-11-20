package com.kinetix.shop.services.outbox

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kinetix.shop.core.config.AppConfigCheckResult
import com.kinetix.shop.core.config.AppConfigValid
import com.kinetix.shop.models.outbox.OutboxFlushOutcome
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OutboxFlushWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val configCheckResult: AppConfigCheckResult,
    private val outboxProcessor: Lazy<OutboxProcessor>,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (configCheckResult !is AppConfigValid) {
            return Result.failure()
        }

        return when (outboxProcessor.get().flushPendingEntries()) {
            OutboxFlushOutcome.COMPLETED -> Result.success()
            OutboxFlushOutcome.RETRY_LATER -> Result.retry()
        }
    }
}
