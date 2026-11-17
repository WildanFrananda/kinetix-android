package com.kinetix.shop.dto.search

import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsResponseDto(
    val suggestions: List<String>,
)
