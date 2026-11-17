package com.kinetix.shop.state.catalog

import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.pricing.ProductPriceIndication
import com.kinetix.shop.models.review.ReviewPage

data class ProductDetailLoadedUiState(
    val product: ProductDetail,
    val priceIndication: ProductPriceIndication,
    val productReviews: ReviewPage?,
    val merchantReviews: ReviewPage?,
) : ProductDetailUiState
