package com.kinetix.shop.ui.screens.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.kinetix.shop.ui.components.HomeCatalogSkeleton
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        when (val current = state) {
            is HomeCatalogLoadingUiState -> {
                HomeCatalogSkeleton()
            }
            is HomeCatalogErrorUiState -> {
                ErrorBannerBlock(
                    failure = current.failure,
                    onRetry = { viewModel.refresh() },
                )
            }
            is HomeCatalogLoadedUiState -> {
                val chipColors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                val hasCachedData = current.productPage.dataOrigin == DataOrigin.CACHE ||
                    current.categories.any { it.dataOrigin == DataOrigin.CACHE }

                if (hasCachedData) {
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
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (current.categoriesUnavailable) {
                    Text(
                        text = stringResource(R.string.home_categories_unavailable),
                        style = MaterialTheme.typography.labelSmall,
                        color = KinetixPalette.WarningAmber,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        val isAllSelected = current.selectedCategorySlug == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { viewModel.selectCategory(null) },
                            shape = MaterialTheme.shapes.small,
                            colors = chipColors,
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllSelected,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp,
                            ),
                            label = {
                                Text(
                                    text = stringResource(R.string.home_category_all),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                    items(current.categories) { category ->
                        val isSelected = current.selectedCategorySlug == category.slug
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(category.slug) },
                            shape = MaterialTheme.shapes.small,
                            colors = chipColors,
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp,
                            ),
                            label = {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (current.productPage.items.isEmpty()) {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.home_empty_products),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    val page = current.productPage
                    val totalPages = if (page.pageSize <= 0) {
                        1
                    } else {
                        ((page.totalCount + page.pageSize - 1) / page.pageSize).coerceAtLeast(1)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(
                            items = page.items,
                            key = { product -> product.sku.value },
                        ) { product ->
                            ProductSummaryCard(
                                item = product,
                                onClick = { onSelectProduct(product.sku) },
                            )
                        }
                        item(span = { GridItemSpan(2) }) {
                            PaginationControlsRow(
                                currentPage = page.page,
                                totalPages = totalPages,
                                onPrevious = {
                                    viewModel.goToPage(page.page - 1)
                                },
                                onNext = {
                                    viewModel.goToPage(page.page + 1)
                                },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
