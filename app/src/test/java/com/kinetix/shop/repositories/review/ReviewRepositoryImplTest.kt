package com.kinetix.shop.repositories.review

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.review.ProductReviewsResponseDto
import com.kinetix.shop.dto.review.ReviewItemDto
import com.kinetix.shop.fakes.FakeReviewApiService
import com.kinetix.shop.models.ids.ProductSku
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ReviewRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(ApiErrorParser(json)))

    @Test
    fun getProductReviewsMapsRatingSummaryAndItems() = runTest {
        val apiService = FakeReviewApiService().apply {
            listProductReviewsHandler = { sku, page ->
                Response.success(
                    ProductReviewsResponseDto(
                        productId = sku,
                        averageRating = 4.8,
                        totalReviews = 12,
                        data = listOf(
                            ReviewItemDto(
                                id = "rev-1",
                                productId = sku,
                                rating = 5,
                                comment = "Fast shipping and solid build",
                                createdAt = "2026-10-19T08:00:00Z",
                                merchantPrincipalId = "merch-1",
                            )
                        ),
                        currentPage = page,
                        lastPage = 2,
                    )
                )
            }
        }
        val repository = ReviewRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
        )

        val result = repository.getProductReviews(ProductSku("SKU-1"), page = 1)
        assertTrue(result is Ok)
        val page = (result as Ok).value
        assertEquals(4.8, page.averageRating, 0.001)
        assertEquals(12, page.totalReviews)
        assertEquals(1, page.items.size)
        assertEquals("rev-1", page.items.first().id.value)
    }
}
