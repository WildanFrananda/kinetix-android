package com.kinetix.shop.ui.screens.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetix.shop.R
import com.kinetix.shop.core.format.LocaleFormatter
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.state.catalog.ProductDetailErrorUiState
import com.kinetix.shop.state.catalog.ProductDetailLoadedUiState
import com.kinetix.shop.state.catalog.ProductDetailLoadingUiState
import com.kinetix.shop.ui.components.ErrorBannerBlock
import com.kinetix.shop.ui.components.FailureMessageMapper
import com.kinetix.shop.ui.components.PaginationControlsRow
import com.kinetix.shop.ui.components.ProductDetailSkeleton
import com.kinetix.shop.ui.components.ProductImageBox
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.catalog.ProductDetailViewModel

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    isSignedIn: Boolean = false,
    onRequireSignIn: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedQuantity by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (val current = state) {
            is ProductDetailLoadingUiState -> {
                ProductDetailSkeleton()
            }
            is ProductDetailErrorUiState -> {
                ErrorBannerBlock(
                    failure = current.failure,
                    onRetry = { viewModel.retry() },
                )
            }
            is ProductDetailLoadedUiState -> {
                val product = current.product
                val priceIndication = current.priceIndication
                val hasSavings = priceIndication.effectivePrice != priceIndication.catalogPrice

                if (product.dataOrigin == DataOrigin.CACHE) {
                    Card(
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, KinetixPalette.WarningAmber),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.data_cached_banner),
                            style = MaterialTheme.typography.labelSmall,
                            color = KinetixPalette.WarningAmber,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }

                ProductImageBox(
                    imageUrl = product.imageUrl,
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f),
                )

                Card(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.product_detail_sku_category_format,
                                product.sku.value,
                                product.categoryName.uppercase(),
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.8.sp,
                            ),
                            color = KinetixPalette.TextMuted,
                        )
                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = LocaleFormatter.formatMoney(
                                    amountString = priceIndication.effectivePrice,
                                    currencyCode = priceIndication.currency,
                                ),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (hasSavings) {
                                Text(
                                    text = LocaleFormatter.formatMoney(
                                        amountString = priceIndication.catalogPrice,
                                        currencyCode = priceIndication.currency,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = TextDecoration.LineThrough,
                                    ),
                                    color = KinetixPalette.TextMuted,
                                )
                            }
                        }

                        val flashSale = priceIndication.appliedFlashSale
                        if (flashSale != null) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.product_detail_flash_sale_badge,
                                    flashSale.remainingQuota,
                                    flashSale.title,
                                    flashSale.remainingQuota,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.extraSmall,
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }

                        val appliedDiscount = priceIndication.appliedDiscount
                        if (appliedDiscount != null) {
                            Text(
                                text = stringResource(
                                    R.string.product_detail_discount_badge,
                                    appliedDiscount.title,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }

                        Text(
                            text = stringResource(R.string.product_detail_indication_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = KinetixPalette.TextMuted,
                        )

                        val availableQty = product.warehouseStock.availableQuantity
                        val stockText = when (product.warehouseStock.status) {
                            StockStatus.IN_STOCK -> {
                                if (availableQty != null) {
                                    pluralStringResource(
                                        R.plurals.product_detail_warehouse_available,
                                        availableQty,
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
                            StockStatus.IN_STOCK -> MaterialTheme.colorScheme.tertiary
                            StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
                            StockStatus.UNKNOWN -> KinetixPalette.WarningAmber
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(stockColor),
                            )
                            Text(
                                text = stockText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                        )

                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(
                                        enabled = !current.isAddingToCart && selectedQuantity > 1,
                                        onClick = {
                                            selectedQuantity = (selectedQuantity - 1).coerceAtLeast(1)
                                        },
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Text(
                                            text = "-",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Text(
                                        text = selectedQuantity.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                    )
                                    IconButton(
                                        enabled = !current.isAddingToCart && selectedQuantity < 999,
                                        onClick = {
                                            selectedQuantity = (selectedQuantity + 1).coerceAtMost(999)
                                        },
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Text(
                                            text = "+",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Button(
                                shape = MaterialTheme.shapes.small,
                                enabled = !current.isAddingToCart &&
                                    product.warehouseStock.status != StockStatus.OUT_OF_STOCK,
                                onClick = {
                                    if (isSignedIn) {
                                        viewModel.addToCart(selectedQuantity)
                                    } else {
                                        onRequireSignIn()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Text(
                                    text = if (current.isAddingToCart) {
                                        stringResource(R.string.product_detail_adding_to_cart)
                                    } else {
                                        stringResource(R.string.product_detail_add_to_cart)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }

                        if (current.addedToCartBanner) {
                            Text(
                                text = stringResource(R.string.product_detail_added_to_cart_banner),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        val addToCartFailure = current.addToCartFailure
                        if (addToCartFailure != null) {
                            Text(
                                text = FailureMessageMapper.resolve(addToCartFailure),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                val merchantReviews = current.merchantReviews
                if (merchantReviews != null) {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.product_detail_merchant_rating_format,
                                    merchantReviews.totalReviews,
                                    merchantReviews.averageRating,
                                    merchantReviews.totalReviews,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    R.string.product_detail_merchant_principal_format,
                                    product.merchantPrincipalId.value,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = KinetixPalette.TextMuted,
                            )
                        }
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (productReviews == null) {
                            Text(
                                text = stringResource(R.string.product_detail_reviews_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.product_detail_reviews_summary_format,
                                    productReviews.totalReviews,
                                    productReviews.averageRating,
                                    productReviews.totalReviews,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (productReviews.items.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.product_detail_reviews_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                productReviews.items.forEachIndexed { index, review ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.product_detail_review_rating_format,
                                                review.rating,
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        val commentText = review.comment
                                        if (!commentText.isNullOrBlank()) {
                                            Text(
                                                text = commentText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Text(
                                            text = LocaleFormatter.formatIsoTimestamp(review.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KinetixPalette.TextMuted,
                                        )
                                    }
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
