package com.kinetix.shop.dto.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiErrorResponseDto(
    val error: String? = null,
    val message: JsonElement? = null,
    val field: String? = null,
    val statusCode: Int? = null,
    val retryAfterSeconds: Int? = null,
    val traceId: String? = null,
)
