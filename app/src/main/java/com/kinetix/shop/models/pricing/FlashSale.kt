package com.kinetix.shop.models.pricing

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku

data class FlashSale(
    val id: String,
    val title: String,
    val productSku: ProductSku,
    val flashPrice: String,
    val stockLimit: Int,
    val stockSold: Int,
    val active: Boolean,
    val startTime: String,
    val endTime: String,
    val merchantPrincipalId: PrincipalId,
) {
    val remainingQuota: Int
        get() = (stockLimit - stockSold).coerceAtLeast(0)
}
