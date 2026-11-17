package com.kinetix.shop.dto.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchIndexDto(
    @SerialName("indexed_through_ms")
    val indexedThroughMs: Long? = null,
    @SerialName("age_seconds")
    val ageSeconds: Long? = null,
    val stale: Boolean,
)
