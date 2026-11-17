package com.kinetix.shop.ui.screens.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kinetix.shop.R
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.state.catalog.ProductDetailErrorUiState
import com.kinetix.shop.state.catalog.ProductDetailLoadedUiState
import com.kinetix.shop.state.catalog.ProductDetailLoadingUiState
import com.kinetix.shop.ui.components.ErrorBannerBlock
import com.kinetix.shop.ui.components.LoadingIndicatorBlock
import com.kinetix.shop.ui.components.PaginationControlsRow
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.catalog.ProductDetailViewModel

@Composable
fun ProductDetailScreen(
    sku: ProductSku,
    viewModel: ProductDetailViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sku) {
        viewModel.loadProduct(sku)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (val current = state) {
            is ProductDetailLoadingUiState -> {
                LoadingIndicatorBlock(
                    label = stringResource(R.string.product_detail_loading),
                )
            }
            is ProductDetailErrorUiState -> {
                ErrorBannerBlock(
                    failure = current.failure,
                    onRetry = { viewModel.loadProduct(sku) },
                )
            }
            is ProductDetailLoadedUiState -> {
                val product = current.product
                val priceIndication = current.priceIndication
                val hasSavings = priceIndication.effectivePrice != priceIndication.catalogPrice

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = KinetixPalette.CardSlate,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.product_detail_sku_category_format,
                                product.sku.value,
                                product.categoryName.uppercase(),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = KinetixPalette.CyanPulse,
                        )
                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = KinetixPalette.TextPrimary,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = "${priceIndication.currency} ${priceIndication.effectivePrice}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = KinetixPalette.ElectricVolt,
                            )
                            if (hasSavings) {
                                Text(
                                    text = "${priceIndication.currency} ${priceIndication.catalogPrice}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = TextDecoration.LineThrough,
                                    ),
                                    color = KinetixPalette.TextSecondary,
                                )
                            }
                        }

                        val flashSale = priceIndication.appliedFlashSale
                        if (flashSale != null) {
                            Text(
                                text = stringResource(
                                    R.string.product_detail_flash_sale_badge,
                                    flashSale.title,
                                    flashSale.remainingQuota,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = KinetixPalette.WarningAmber,
                            )
                        }

                        val primaryDiscount = priceIndication.applicableDiscounts.firstOrNull()
                        if (primaryDiscount != null) {
                            Text(
                                text = stringResource(
                                    R.string.product_detail_discount_badge,
                                    primaryDiscount.title,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = KinetixPalette.SuccessEmerald,
                            )
                        }

                        Text(
                            text = stringResource(R.string.product_detail_indication_note),
                            style = MaterialTheme.typography.labelMedium,
                            color = KinetixPalette.TextSecondary,
                        )

                        val availableQty = product.warehouseStock.availableQuantity
                        val stockText = when (product.warehouseStock.status) {
                            StockStatus.IN_STOCK -> {
                                if (availableQty != null) {
                                    stringResource(
                                        R.string.product_detail_warehouse_available,
                                        availableQty,
                                    )
                                } else {
                                    stringResource(R.string.product_detail_warehouse_available_no_count)
                                }
                            }
                            StockStatus.OUT_OF_STOCK -> stringResource(
                                R.string.product_detail_warehouse_out_of_stock,
                            )
                            StockStatus.UNKNOWN -> stringResource(
                                R.string.product_detail_warehouse_unavailable,
                            )
                        }
                        val stockColor = when (product.warehouseStock.status) {
                            StockStatus.IN_STOCK -> KinetixPalette.SuccessEmerald
                            StockStatus.OUT_OF_STOCK -> KinetixPalette.DangerCoral
                            StockStatus.UNKNOWN -> KinetixPalette.WarningAmber
                        }
                        Text(
                            text = stockText,
                            style = MaterialTheme.typography.labelMedium,
                            color = stockColor,
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp),
                        )
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KinetixPalette.TextPrimary,
                        )
                    }
                }

                val merchantReviews = current.merchantReviews
                if (merchantReviews != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = KinetixPalette.CardSlate,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.product_detail_merchant_rating_format,
                                    merchantReviews.averageRating,
                                    merchantReviews.totalReviews,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = KinetixPalette.CyanPulse,
                            )
                            Text(
                                text = stringResource(
                                    R.string.product_detail_merchant_principal_format,
                                    product.merchantPrincipalId.value,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = KinetixPalette.TextSecondary,
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = KinetixPalette.CardSlate,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val productReviews = current.productReviews
                        Text(
                            text = stringResource(R.string.product_detail_reviews_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = KinetixPalette.ElectricVolt,
                        )
                        if (productReviews == null) {
                            Text(
                                text = stringResource(R.string.product_detail_reviews_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = KinetixPalette.TextSecondary,
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.product_detail_reviews_summary_format,
                                    productReviews.averageRating,
                                    productReviews.totalReviews,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = KinetixPalette.TextSecondary,
                            )
                            if (productReviews.items.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.product_detail_reviews_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KinetixPalette.TextSecondary,
                                )
                            } else {
                                productReviews.items.forEach { review ->
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.product_detail_review_rating_format,
                                                review.rating,
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = KinetixPalette.ElectricVolt,
                                        )
                                        val commentText = review.comment
                                        if (!commentText.isNullOrBlank()) {
                                            Text(
                                                text = commentText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = KinetixPalette.TextPrimary,
                                            )
                                        }
                                        Text(
                                            text = review.createdAt,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = KinetixPalette.TextSecondary,
                                        )
                                    }
                                    Spacer(
                                        modifier = Modifier.height(6.dp),
                                    )
                                }

                                PaginationControlsRow(
                                    currentPage = productReviews.currentPage,
                                    totalPages = productReviews.lastPage.coerceAtLeast(1),
                                    onPrevious = {
                                        viewModel.loadReviewsPage(productReviews.currentPage - 1)
                                    },
                                    onNext = {
                                        viewModel.loadReviewsPage(productReviews.currentPage + 1)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
