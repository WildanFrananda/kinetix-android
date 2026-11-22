package com.kinetix.shop.models.review

import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.ids.ReviewId

data class ProductReview(
    val id: ReviewId,
    val productSku: ProductSku,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val merchantPrincipalId: PrincipalId? = null,
)
