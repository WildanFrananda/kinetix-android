package com.kinetix.shop.models.outbox

enum class OutboxFlushOutcome {
    COMPLETED,
    RETRY_LATER,
}
