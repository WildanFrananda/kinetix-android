package com.kinetix.shop.models.catalog

import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku

data class ProductSummary(
    val id: Long,
    val sku: ProductSku,
    val title: String,
    val categoryName: String,
    val categorySlug: CategorySlug,
    val price: String,
    val currency: String,
    val imageUrl: String,
    val merchantPrincipalId: PrincipalId,
    val weightGrams: Int?,
    val availableStock: Int?,
    val stockStatus: StockStatus,
    val dataOrigin: DataOrigin = DataOrigin.LIVE,
    val cachedAtEpochSeconds: Long? = null,
)
