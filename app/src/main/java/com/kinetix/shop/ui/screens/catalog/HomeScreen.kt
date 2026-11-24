package com.kinetix.shop.ui.screens.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetix.shop.R
import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.state.catalog.HomeCatalogErrorUiState
import com.kinetix.shop.state.catalog.HomeCatalogLoadedUiState
import com.kinetix.shop.state.catalog.HomeCatalogLoadingUiState
import com.kinetix.shop.ui.components.ErrorBannerBlock
import com.kinetix.shop.ui.components.LoadingIndicatorBlock
import com.kinetix.shop.ui.components.PaginationControlsRow
import com.kinetix.shop.ui.components.ProductSummaryCard
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.catalog.HomeCatalogViewModel

@Composable
fun HomeScreen(
    viewModel: HomeCatalogViewModel,
    onSelectProduct: (ProductSku) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        when (val current = state) {
            is HomeCatalogLoadingUiState -> {
                LoadingIndicatorBlock(
                    label = stringResource(R.string.home_loading),
                )
            }
            is HomeCatalogErrorUiState -> {
                ErrorBannerBlock(
                    failure = current.failure,
                    onRetry = { viewModel.refresh() },
                )
            }
            is HomeCatalogLoadedUiState -> {
                val hasCachedData = current.productPage.dataOrigin == DataOrigin.CACHE ||
                    current.categories.any { it.dataOrigin == DataOrigin.CACHE }
                if (hasCachedData) {
                    Text(
                        text = stringResource(R.string.data_cached_banner),
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixPalette.WarningAmber,
                    )
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )
                }

                if (current.categoriesUnavailable) {
                    Text(
                        text = stringResource(R.string.home_categories_unavailable),
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixPalette.WarningAmber,
                    )
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        FilterChip(
                            selected = current.selectedCategorySlug == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text(text = stringResource(R.string.home_category_all)) },
                        )
                    }
                    items(current.categories) { category ->
                        FilterChip(
                            selected = current.selectedCategorySlug == category.slug,
                            onClick = { viewModel.selectCategory(category.slug) },
                            label = { Text(text = category.name) },
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp),
                )

                if (current.productPage.items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.home_empty_products),
                        style = MaterialTheme.typography.bodyMedium,
                        color = KinetixPalette.TextSecondary,
                    )
                } else {
                    val page = current.productPage
                    val totalPages = if (page.pageSize <= 0) {
                        1
                    } else {
                        ((page.totalCount + page.pageSize - 1) / page.pageSize).coerceAtLeast(1)
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(page.items) { product ->
                            ProductSummaryCard(
                                item = product,
                                onClick = { onSelectProduct(product.sku) },
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )

                    PaginationControlsRow(
                        currentPage = page.page,
                        totalPages = totalPages,
                        onPrevious = {
                            viewModel.goToPage(page.page - 1)
                        },
                        onNext = {
                            viewModel.goToPage(page.page + 1)
                        },
                    )
                }
            }
        }
    }
}
