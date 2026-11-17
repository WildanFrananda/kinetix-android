package com.kinetix.shop.fakes

import com.kinetix.shop.dto.review.MerchantReviewsResponseDto
import com.kinetix.shop.dto.review.ProductReviewsResponseDto
import com.kinetix.shop.services.remote.ReviewApiService
import retrofit2.Response

class FakeReviewApiService : ReviewApiService {
    var listProductReviewsHandler: (String, Int) -> Response<ProductReviewsResponseDto> = { _, _ ->
        throw UnsupportedOperationException("listProductReviewsHandler not configured")
    }
    var listMerchantReviewsHandler: (String, Int) -> Response<MerchantReviewsResponseDto> = { _, _ ->
        throw UnsupportedOperationException("listMerchantReviewsHandler not configured")
    }

    override suspend fun listProductReviews(
        productSku: String,
        page: Int,
    ): Response<ProductReviewsResponseDto> {
        return listProductReviewsHandler(productSku, page)
    }

    override suspend fun listMerchantReviews(
        merchantPrincipalId: String,
        page: Int,
    ): Response<MerchantReviewsResponseDto> {
        return listMerchantReviewsHandler(merchantPrincipalId, page)
    }
}
