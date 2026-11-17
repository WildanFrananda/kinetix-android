package com.kinetix.shop.models.review

data class ReviewPage(
    val averageRating: Double,
    val totalReviews: Int,
    val currentPage: Int,
    val lastPage: Int,
    val items: List<ProductReview>,
) {
    val hasNextPage: Boolean
        get() = currentPage < lastPage
}
