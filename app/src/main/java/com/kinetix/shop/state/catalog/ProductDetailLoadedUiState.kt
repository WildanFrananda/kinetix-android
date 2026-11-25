package com.kinetix.shop.state.catalog

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.pricing.ProductPriceIndication
import com.kinetix.shop.models.review.ReviewPage

data class ProductDetailLoadedUiState(
    val product: ProductDetail,
    val priceIndication: ProductPriceIndication,
    val productReviews: ReviewPage?,
    val merchantReviews: ReviewPage?,
    val isAddingToCart: Boolean = false,
    val addedToCartBanner: Boolean = false,
    val addToCartFailure: Failure? = null,
) : ProductDetailUiState
