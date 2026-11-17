package com.kinetix.shop.models.catalog

data class ProductPage(
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val items: List<ProductSummary>,
) {
    val hasNextPage: Boolean
        get() = page * pageSize < totalCount
}
