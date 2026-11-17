package com.kinetix.shop.repositories.review

import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.dto.review.ReviewItemDto
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.ids.ReviewId
import com.kinetix.shop.models.review.ProductReview
import com.kinetix.shop.models.review.ReviewPage
import com.kinetix.shop.services.remote.ReviewApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val apiService: ReviewApiService,
    private val safeApiCaller: SafeApiCaller,
) : ReviewRepository {
    override suspend fun getProductReviews(
        sku: ProductSku,
        page: Int,
    ): Result<ReviewPage> {
        return safeApiCaller.execute(
            call = {
                apiService.listProductReviews(
                    productSku = sku.value,
                    page = page,
                )
            },
            transform = { dto ->
                Ok(
                    ReviewPage(
                        averageRating = dto.averageRating,
                        totalReviews = dto.totalReviews,
                        currentPage = dto.currentPage,
                        lastPage = dto.lastPage,
                        items = dto.data.map { mapReview(it) },
                    )
                )
            },
        )
    }

    override suspend fun getMerchantReviews(
        merchantPrincipalId: PrincipalId,
        page: Int,
    ): Result<ReviewPage> {
        return safeApiCaller.execute(
            call = {
                apiService.listMerchantReviews(
                    merchantPrincipalId = merchantPrincipalId.value,
                    page = page,
                )
            },
            transform = { dto ->
                Ok(
                    ReviewPage(
                        averageRating = dto.averageRating,
                        totalReviews = dto.totalReviews,
                        currentPage = dto.currentPage,
                        lastPage = dto.lastPage,
                        items = dto.data.map { mapReview(it) },
                    )
                )
            },
        )
    }

    private fun mapReview(
        dto: ReviewItemDto,
    ): ProductReview {
        return ProductReview(
            id = ReviewId(dto.id),
            productSku = ProductSku(dto.productId),
            rating = dto.rating,
            comment = dto.comment,
            createdAt = dto.createdAt,
            merchantPrincipalId = PrincipalId(dto.merchantPrincipalId),
        )
    }
}
