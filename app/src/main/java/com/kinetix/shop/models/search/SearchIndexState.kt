package com.kinetix.shop.models.search

data class SearchIndexState(
    val indexedThroughEpochMillis: Long?,
    val ageSeconds: Long?,
    val stale: Boolean,
)
