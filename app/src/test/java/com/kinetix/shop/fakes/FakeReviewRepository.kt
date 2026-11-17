package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.review.ReviewPage
import com.kinetix.shop.repositories.review.ReviewRepository

class FakeReviewRepository : ReviewRepository {
    var productReviewsResult: Result<ReviewPage> = Err(
        UnavailableFailure("NOT_CONFIGURED", null)
    )
    var merchantReviewsResult: Result<ReviewPage> = Err(
        UnavailableFailure("NOT_CONFIGURED", null)
    )

    override suspend fun getProductReviews(
        sku: ProductSku,
        page: Int,
    ): Result<ReviewPage> {
        return productReviewsResult
    }

    override suspend fun getMerchantReviews(
        merchantPrincipalId: PrincipalId,
        page: Int,
    ): Result<ReviewPage> {
        return merchantReviewsResult
    }
}
