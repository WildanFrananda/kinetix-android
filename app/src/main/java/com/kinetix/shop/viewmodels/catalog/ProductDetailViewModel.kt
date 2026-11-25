package com.kinetix.shop.viewmodels.catalog

import android.annotation.SuppressLint
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.pricing.ProductPriceIndication
import com.kinetix.shop.repositories.cart.CartRepository
import com.kinetix.shop.repositories.catalog.CatalogRepository
import com.kinetix.shop.repositories.pricing.PricingRepository
import com.kinetix.shop.repositories.review.ReviewRepository
import com.kinetix.shop.state.catalog.ProductDetailErrorUiState
import com.kinetix.shop.state.catalog.ProductDetailLoadedUiState
import com.kinetix.shop.state.catalog.ProductDetailLoadingUiState
import com.kinetix.shop.state.catalog.ProductDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val pricingRepository: PricingRepository,
    private val reviewRepository: ReviewRepository,
    private val cartRepository: CartRepository?,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    @SuppressLint("VisibleForTests")
    constructor(
        catalogRepository: CatalogRepository,
        pricingRepository: PricingRepository,
        reviewRepository: ReviewRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) : this(
        catalogRepository = catalogRepository,
        pricingRepository = pricingRepository,
        reviewRepository = reviewRepository,
        cartRepository = null,
        savedStateHandle = savedStateHandle,
    )

    private val mutableState = MutableStateFlow<ProductDetailUiState>(ProductDetailLoadingUiState)
    val uiState: StateFlow<ProductDetailUiState> = mutableState.asStateFlow()

    private var activeSku: ProductSku? = null
    private var loadJob: Job? = null
    private var reviewsJob: Job? = null
    private var pendingReconciliationSku: ProductSku? = null

    init {
        val routeSku = savedStateHandle.get<String>("sku")?.takeIf { it.isNotBlank() }
        if (routeSku != null) {
            loadProduct(ProductSku(routeSku))
        }
    }

    fun loadProduct(
        sku: ProductSku,
    ) {
        if (activeSku == sku && (loadJob?.isActive == true || mutableState.value is ProductDetailLoadedUiState)) {
            return
        }
        startLoad(sku)
    }

    fun retry() {
        val sku = activeSku ?: return
        startLoad(sku)
    }

    fun addToCart(
        quantity: Int,
    ) {
        val repository = cartRepository ?: return
        val current = mutableState.value as? ProductDetailLoadedUiState ?: return
        if (current.isAddingToCart) {
            return
        }
        val clamped = quantity.coerceIn(1, MAX_ADD_QUANTITY)
        mutableState.value = current.copy(
            isAddingToCart = true,
            addedToCartBanner = false,
            addToCartFailure = null,
        )
        viewModelScope.launch {
            if (pendingReconciliationSku == current.product.sku) {
                when (val cartCheck = repository.getCart()) {
                    is Ok -> pendingReconciliationSku = null
                    is Err -> {
                        val latest = mutableState.value as? ProductDetailLoadedUiState ?: return@launch
                        mutableState.value = latest.copy(
                            isAddingToCart = false,
                            addedToCartBanner = false,
                            addToCartFailure = cartCheck.failure,
                        )
                        return@launch
                    }
                }
            }
            when (val result = repository.addCartItem(sku = current.product.sku, quantity = clamped)) {
                is Ok -> {
                    pendingReconciliationSku = null
                    val latest = mutableState.value as? ProductDetailLoadedUiState ?: return@launch
                    mutableState.value = latest.copy(
                        isAddingToCart = false,
                        addedToCartBanner = true,
                        addToCartFailure = null,
                    )
                }
                is Err -> {
                    if (result.failure is com.kinetix.shop.core.failure.UnknownOutcomeFailure) {
                        pendingReconciliationSku = current.product.sku
                    }
                    val latest = mutableState.value as? ProductDetailLoadedUiState ?: return@launch
                    mutableState.value = latest.copy(
                        isAddingToCart = false,
                        addedToCartBanner = false,
                        addToCartFailure = result.failure,
                    )
                }
            }
        }
    }

    fun loadReviewsPage(
        page: Int,
    ) {
        val current = mutableState.value as? ProductDetailLoadedUiState ?: return
        reviewsJob?.cancel()
        reviewsJob = viewModelScope.launch {
            when (
                val revResult = reviewRepository.getProductReviews(
                    sku = current.product.sku,
                    page = page.coerceAtLeast(1),
                )
            ) {
                is Ok -> {
                    val latest = mutableState.value as? ProductDetailLoadedUiState ?: return@launch
                    mutableState.value = latest.copy(
                        productReviews = revResult.value,
                    )
                }
                is Err -> Unit
            }
        }
    }

    private fun startLoad(
        sku: ProductSku,
    ) {
        activeSku = sku
        loadJob?.cancel()
        reviewsJob?.cancel()
        mutableState.value = ProductDetailLoadingUiState
        loadJob = viewModelScope.launch {
            val product = when (val detailResult = catalogRepository.getProductDetail(sku)) {
                is Ok -> detailResult.value
                is Err -> {
                    mutableState.value = ProductDetailErrorUiState(failure = detailResult.failure)
                    return@launch
                }
            }

            val priceDeferred = async {
                pricingRepository.computePriceIndication(
                    sku = product.sku,
                    categorySlug = product.categorySlug,
                    merchantPrincipalId = product.merchantPrincipalId,
                    catalogPrice = product.price,
                    currency = product.currency,
                )
            }
            val productReviewsDeferred = async {
                reviewRepository.getProductReviews(
                    sku = product.sku,
                    page = 1,
                )
            }
            val merchantReviewsDeferred = async {
                reviewRepository.getMerchantReviews(
                    merchantPrincipalId = product.merchantPrincipalId,
                    page = 1,
                )
            }

            val indication = when (val priceResult = priceDeferred.await()) {
                is Ok -> priceResult.value
                is Err -> ProductPriceIndication(
                    catalogPrice = product.price,
                    effectivePrice = product.price,
                    currency = product.currency,
                    appliedFlashSale = null,
                    appliedDiscount = null,
                    applicableDiscounts = emptyList(),
                )
            }

            val productReviews = when (val revResult = productReviewsDeferred.await()) {
                is Ok -> revResult.value
                is Err -> null
            }

            val merchantReviews = when (val merchResult = merchantReviewsDeferred.await()) {
                is Ok -> merchResult.value
                is Err -> null
            }

            mutableState.value = ProductDetailLoadedUiState(
                product = product,
                priceIndication = indication,
                productReviews = productReviews,
                merchantReviews = merchantReviews,
            )
        }
    }

    companion object {
        private const val MAX_ADD_QUANTITY = 999
    }
}
