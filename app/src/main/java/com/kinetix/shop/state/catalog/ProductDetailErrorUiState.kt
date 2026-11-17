package com.kinetix.shop.state.catalog

import com.kinetix.shop.core.failure.Failure

data class ProductDetailErrorUiState(
    val failure: Failure,
) : ProductDetailUiState
