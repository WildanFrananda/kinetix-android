package com.kinetix.shop.models.outbox

enum class OutboxStatus {
    PENDING,
    IN_FLIGHT,
    FAILED_PERMANENT,
}
