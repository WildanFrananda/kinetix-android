package com.kinetix.shop.models.pricing

import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku

data class PromotionDiscount(
    val id: String,
    val title: String,
    val discountType: DiscountType,
    val value: String,
    val targetProductSku: ProductSku?,
    val targetCategorySlug: CategorySlug?,
    val active: Boolean,
    val startTime: String,
    val endTime: String,
    val merchantPrincipalId: PrincipalId?,
)
