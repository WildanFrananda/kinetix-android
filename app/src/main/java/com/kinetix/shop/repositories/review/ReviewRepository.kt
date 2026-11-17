package com.kinetix.shop.repositories.review

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.review.ReviewPage

interface ReviewRepository {
    suspend fun getProductReviews(
        sku: ProductSku,
        page: Int = 1,
    ): Result<ReviewPage>

    suspend fun getMerchantReviews(
        merchantPrincipalId: PrincipalId,
        page: Int = 1,
    ): Result<ReviewPage>
}
