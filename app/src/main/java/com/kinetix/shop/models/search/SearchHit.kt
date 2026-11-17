package com.kinetix.shop.models.search

import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku

data class SearchHit(
    val sku: ProductSku,
    val merchantPrincipalId: PrincipalId,
    val title: String,
    val description: String?,
    val categories: List<CategorySlug>,
    val priceMinor: Long,
    val currency: String,
)
