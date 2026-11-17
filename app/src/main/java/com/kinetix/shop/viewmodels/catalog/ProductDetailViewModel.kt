package com.kinetix.shop.viewmodels.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.pricing.ProductPriceIndication
import com.kinetix.shop.repositories.catalog.CatalogRepository
import com.kinetix.shop.repositories.pricing.PricingRepository
import com.kinetix.shop.repositories.review.ReviewRepository
import com.kinetix.shop.state.catalog.ProductDetailErrorUiState
import com.kinetix.shop.state.catalog.ProductDetailLoadedUiState
import com.kinetix.shop.state.catalog.ProductDetailLoadingUiState
import com.kinetix.shop.state.catalog.ProductDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
) : ViewModel() {
    private val mutableState = MutableStateFlow<ProductDetailUiState>(ProductDetailLoadingUiState)
    val uiState: StateFlow<ProductDetailUiState> = mutableState.asStateFlow()

    fun loadProduct(
        sku: ProductSku,
    ) {
        mutableState.value = ProductDetailLoadingUiState
        viewModelScope.launch {
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

    fun loadReviewsPage(
        page: Int,
    ) {
        val current = mutableState.value as? ProductDetailLoadedUiState ?: return
        viewModelScope.launch {
            when (
                val revResult = reviewRepository.getProductReviews(
                    sku = current.product.sku,
                    page = page.coerceAtLeast(1),
                )
            ) {
                is Ok -> {
                    mutableState.value = current.copy(
                        productReviews = revResult.value,
                    )
                }
                is Err -> Unit
            }
        }
    }
}
