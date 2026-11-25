package com.kinetix.shop.state.cart

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.models.cart.Cart
import com.kinetix.shop.models.pricing.VoucherTerms

data class CartLoadedUiState(
    val cart: Cart,
    val isMutating: Boolean = false,
    val actionFailure: Failure? = null,
    val voucherTerms: VoucherTerms? = null,
    val voucherPreviewAccepted: Boolean = false,
    val voucherPreviewRejected: Boolean = false,
) : CartUiState
