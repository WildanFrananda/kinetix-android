package com.kinetix.shop.dto.review

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductReviewsResponseDto(
    @SerialName("product_id")
    val productId: String,
    @SerialName("average_rating")
    val averageRating: Double,
    @SerialName("total_reviews")
    val totalReviews: Int,
    val data: List<ReviewItemDto>,
    @SerialName("current_page")
    val currentPage: Int,
    @SerialName("last_page")
    val lastPage: Int,
)
