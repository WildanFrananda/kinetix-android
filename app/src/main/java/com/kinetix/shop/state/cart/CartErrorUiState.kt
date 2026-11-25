package com.kinetix.shop.state.cart

import com.kinetix.shop.core.failure.Failure

data class CartErrorUiState(
    val failure: Failure,
) : CartUiState
