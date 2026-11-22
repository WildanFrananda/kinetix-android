package com.kinetix.shop.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiErrorResponseDto(
    val error: String? = null,
    val code: String? = null,
    val message: JsonElement? = null,
    val field: String? = null,
    val statusCode: Int? = null,
    val retryAfterSeconds: Int? = null,
    val traceId: String? = null,
    val reason: String? = null,
    val orderNumber: String? = null,
    val availableTiers: List<String>? = null,
    val replayed: Boolean? = null,
    val rolledBack: Boolean? = null,
    val limit: String? = null,
    val referenceNumber: String? = null,
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    @SerialName("request_id")
    val requestId: String? = null,
)
