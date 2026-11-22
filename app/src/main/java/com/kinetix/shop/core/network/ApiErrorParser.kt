package com.kinetix.shop.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

@Singleton
class ApiErrorParser @Inject constructor(
    private val json: Json,
) {
    fun parse(
        rawBody: String?,
    ): ApiErrorResponseDto? {
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

    fun extractErrorCode(
        dto: ApiErrorResponseDto?,
    ): String? {
        if (dto == null) {
            return null
        }

        val explicitCode = dto.code?.takeIf { it.isNotBlank() }
        if (explicitCode != null) {
            return explicitCode
        }

        val rawError = dto.error?.takeIf { it.isNotBlank() } ?: return null
        if (dto.statusCode != null) {
            return null
        }
        if (rawError.contains(' ')) {
            return null
        }

        return rawError
    }

    fun isServiceJsonError(
        dto: ApiErrorResponseDto?,
    ): Boolean {
        if (dto == null) {
            return false
        }

        return !dto.error.isNullOrBlank() ||
            !dto.code.isNullOrBlank() ||
            !dto.title.isNullOrBlank() ||
            dto.status != null
    }

    fun extractMessage(
        dto: ApiErrorResponseDto?,
    ): String? {
        if (dto == null) {
            return null
        }

        val element = dto.message
        if (element != null) {
            return when (element) {
                is JsonPrimitive -> element.content.takeIf { it.isNotBlank() }
                is JsonArray -> {
                    element.mapNotNull { item ->
                        (item as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                    }.joinToString("; ").takeIf { it.isNotBlank() }
                }

                else -> element.toString()
            }
        }

        val detail = dto.detail?.takeIf { it.isNotBlank() }
        if (detail != null) {
            return detail
        }

        val title = dto.title?.takeIf { it.isNotBlank() }
        if (title != null) {
            return title
        }

        val sentenceError = dto.error?.takeIf { it.contains(' ') }
        if (sentenceError != null) {
            return sentenceError
        }

        return null
    }
}
