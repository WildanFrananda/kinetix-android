package com.kinetix.shop.models.catalog

import com.kinetix.shop.models.common.DataOrigin

data class ProductPage(
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val items: List<ProductSummary>,
    val dataOrigin: DataOrigin = DataOrigin.LIVE,
    val cachedAtEpochSeconds: Long? = null,
) {
    val hasNextPage: Boolean
        get() = page * pageSize < totalCount
}
