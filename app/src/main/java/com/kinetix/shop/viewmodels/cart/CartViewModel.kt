package com.kinetix.shop.viewmodels.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.failure.ValidationFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.models.cart.Cart
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.ids.VoucherCode
import com.kinetix.shop.repositories.cart.CartRepository
import com.kinetix.shop.repositories.pricing.PricingRepository
import com.kinetix.shop.state.cart.CartErrorUiState
import com.kinetix.shop.state.cart.CartLoadedUiState
import com.kinetix.shop.state.cart.CartLoadingUiState
import com.kinetix.shop.state.cart.CartUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val pricingRepository: PricingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<CartUiState>(CartLoadingUiState)
    val uiState: StateFlow<CartUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var mutationJob: Job? = null

    init {
        startLoad()
    }

    fun refresh() {
        startLoad()
    }

    fun updateItemQuantity(
        sku: ProductSku,
        quantity: Int,
    ) {
        if (quantity <= 0) {
            removeItem(sku)
            return
        }
        val current = mutableState.value as? CartLoadedUiState ?: return
        if (mutationJob?.isActive == true) {
            return
        }
        val clamped = quantity.coerceIn(1, MAX_LINE_QUANTITY)
        mutableState.value = current.copy(
            isMutating = true,
            actionFailure = null,
        )
        mutationJob = viewModelScope.launch {
            applyCartResult(
                previous = current,
                result = cartRepository.updateCartItem(sku = sku, quantity = clamped),
            )
        }
    }

    fun removeItem(
        sku: ProductSku,
    ) {
        val current = mutableState.value as? CartLoadedUiState ?: return
        if (mutationJob?.isActive == true) {
            return
        }
        mutableState.value = current.copy(
            isMutating = true,
            actionFailure = null,
        )
        mutationJob = viewModelScope.launch {
            applyCartResult(
                previous = current,
                result = cartRepository.removeCartItem(sku = sku),
            )
        }
    }

    fun clearCart() {
        val current = mutableState.value as? CartLoadedUiState ?: return
        if (mutationJob?.isActive == true) {
            return
        }
        mutableState.value = current.copy(
            isMutating = true,
            actionFailure = null,
        )
        mutationJob = viewModelScope.launch {
            when (val result = cartRepository.clearCart()) {
                is Ok -> {
                    mutableState.value = CartLoadedUiState(
                        cart = Cart(
                            customerPrincipalId = current.cart.customerPrincipalId,
                            items = emptyList(),
                            appliedVoucherCode = null,
                            subtotal = java.math.BigDecimal.ZERO,
                            updatedAt = current.cart.updatedAt,
                        ),
                    )
                }
                is Err -> {
                    handleMutationFailure(current, result.failure)
                }
            }
        }
    }

    fun applyVoucher(
        rawCode: String,
    ) {
        val current = mutableState.value as? CartLoadedUiState ?: return
        val trimmed = rawCode.trim()
        if (trimmed.isEmpty()) {
            mutableState.value = current.copy(
                actionFailure = ValidationFailure(code = "VOUCHER_CODE_REQUIRED"),
            )
            return
        }
        if (trimmed.length > MAX_VOUCHER_LENGTH) {
            mutableState.value = current.copy(
                actionFailure = ValidationFailure(code = "FIELD_TOO_LONG"),
            )
            return
        }
        if (mutationJob?.isActive == true) {
            return
        }

        val code = VoucherCode(trimmed)
        mutableState.value = current.copy(
            isMutating = true,
            actionFailure = null,
            voucherPreviewAccepted = false,
            voucherPreviewRejected = false,
        )
        mutationJob = viewModelScope.launch {
            val terms = when (val termsResult = pricingRepository.getVoucherTerms(code)) {
                is Ok -> termsResult.value
                is Err -> null
            }
            val previewResult = pricingRepository.previewVoucher(
                code = code,
                cartSubtotal = current.cart.subtotal,
                merchantPrincipalId = current.cart.singleMerchantPrincipalId(),
            )
            val previewTerms = when (previewResult) {
                is Ok -> previewResult.value
                is Err -> null
            }
            val previewAccepted = previewResult is Ok && previewTerms != null
            val previewRejected = previewResult is Ok && previewTerms == null

            when (val applyResult = cartRepository.applyCartVoucher(code)) {
                is Ok -> {
                    mutableState.value = CartLoadedUiState(
                        cart = applyResult.value,
                        isMutating = false,
                        voucherTerms = previewTerms ?: terms,
                        voucherPreviewAccepted = previewAccepted,
                        voucherPreviewRejected = previewRejected,
                    )
                }
                is Err -> {
                    handleMutationFailure(
                        previous = current.copy(
                            voucherTerms = previewTerms ?: terms,
                            voucherPreviewAccepted = previewAccepted,
                            voucherPreviewRejected = previewRejected,
                        ),
                        failure = applyResult.failure,
                    )
                }
            }
        }
    }

    fun removeVoucher() {
        val current = mutableState.value as? CartLoadedUiState ?: return
        if (mutationJob?.isActive == true) {
            return
        }
        mutableState.value = current.copy(
            isMutating = true,
            actionFailure = null,
        )
        mutationJob = viewModelScope.launch {
            applyCartResult(
                previous = current,
                result = cartRepository.removeCartVoucher(),
            )
        }
    }

    private suspend fun applyCartResult(
        previous: CartLoadedUiState,
        result: com.kinetix.shop.core.result.Result<Cart>,
    ) {
        when (result) {
            is Ok -> {
                mutableState.value = previous.copy(
                    cart = result.value,
                    isMutating = false,
                    actionFailure = null,
                )
            }
            is Err -> {
                handleMutationFailure(previous, result.failure)
            }
        }
    }

    private suspend fun handleMutationFailure(
        previous: CartLoadedUiState,
        failure: com.kinetix.shop.core.failure.Failure,
    ) {
        if (failure is UnknownOutcomeFailure) {
            when (val refreshed = cartRepository.getCart()) {
                is Ok -> {
                    mutableState.value = previous.copy(
                        cart = refreshed.value,
                        isMutating = false,
                        actionFailure = failure,
                    )
                }
                is Err -> {
                    mutableState.value = previous.copy(
                        isMutating = false,
                        actionFailure = failure,
                    )
                }
            }
            return
        }

        mutableState.value = previous.copy(
            isMutating = false,
            actionFailure = failure,
        )
    }

    private fun startLoad() {
        loadJob?.cancel()
        mutableState.value = CartLoadingUiState
        loadJob = viewModelScope.launch {
            when (val result = cartRepository.getCart()) {
                is Ok -> {
                    val cart = result.value
                    val existingVoucher = cart.appliedVoucherCode
                    val voucherTerms = if (existingVoucher != null) {
                        when (val termsResult = pricingRepository.getVoucherTerms(existingVoucher)) {
                            is Ok -> termsResult.value
                            is Err -> null
                        }
                    } else {
                        null
                    }
                    mutableState.value = CartLoadedUiState(
                        cart = cart,
                        voucherTerms = voucherTerms,
                    )
                }
                is Err -> {
                    mutableState.value = CartErrorUiState(failure = result.failure)
                }
            }
        }
    }

    companion object {
        private const val MAX_LINE_QUANTITY = 999
        private const val MAX_VOUCHER_LENGTH = 50
    }
}
