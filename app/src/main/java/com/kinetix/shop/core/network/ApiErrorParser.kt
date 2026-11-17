package com.kinetix.shop.core.network

import com.kinetix.shop.dto.common.ApiErrorResponseDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

@Singleton
class ApiErrorParser @Inject constructor(
    private val json: Json,
) {
    fun parse(rawBody: String?): ApiErrorResponseDto? {
        if (rawBody.isNullOrBlank()) {
            return null
        }

        return try {
            json.decodeFromString(
                ApiErrorResponseDto.serializer(),
                rawBody,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun extractMessage(dto: ApiErrorResponseDto?): String? {
        val element = dto?.message ?: return null
        return when (element) {
            is JsonPrimitive -> element.content
            is JsonArray -> {
                element.mapNotNull { item ->
                    (item as? JsonPrimitive)?.content
                }.joinToString("; ")
            }

            else -> element.toString()
        }
    }
}
