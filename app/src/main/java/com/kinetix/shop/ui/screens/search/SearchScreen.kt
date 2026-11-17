package com.kinetix.shop.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinetix.shop.R
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.state.search.SearchErrorUiState
import com.kinetix.shop.state.search.SearchIdleUiState
import com.kinetix.shop.state.search.SearchLoadedUiState
import com.kinetix.shop.state.search.SearchLoadingUiState
import com.kinetix.shop.ui.components.ErrorBannerBlock
import com.kinetix.shop.ui.components.LoadingIndicatorBlock
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.search.SearchViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onSelectProduct: (ProductSku) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var queryInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = queryInput,
                onValueChange = { input ->
                    queryInput = input
                    viewModel.updateQueryAndSuggest(input)
                },
                label = { Text(text = stringResource(R.string.search_field_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    viewModel.executeSearch(query = queryInput)
                },
            ) {
                Text(
                    text = stringResource(R.string.search_button),
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        val sortOptions = listOf(
            null to stringResource(R.string.search_sort_relevance),
            "price_asc" to stringResource(R.string.search_sort_price_asc),
            "price_desc" to stringResource(R.string.search_sort_price_desc),
            "newest" to stringResource(R.string.search_sort_newest),
        )

        val loadedState = state as? SearchLoadedUiState

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(sortOptions) { (sortKey, label) ->
                FilterChip(
                    selected = loadedState?.selectedSort == sortKey,
                    onClick = {
                        viewModel.executeSearch(
                            query = queryInput,
                            sort = sortKey,
                        )
                    },
                    label = { Text(text = label) },
                )
            }
        }

        if (loadedState != null && loadedState.suggestions.isNotEmpty()) {
            Spacer(
                modifier = Modifier.height(6.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(loadedState.suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = {
                            queryInput = suggestion
                            viewModel.executeSearch(query = suggestion)
                        },
                        label = { Text(text = suggestion) },
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp),
        )

        when (val current = state) {
            is SearchIdleUiState -> {
                Text(
                    text = stringResource(R.string.search_idle_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = KinetixPalette.TextSecondary,
                )
            }
            is SearchLoadingUiState -> {
                LoadingIndicatorBlock(
                    label = stringResource(R.string.search_loading),
                )
            }
            is SearchErrorUiState -> {
                ErrorBannerBlock(
                    failure = current.failure,
                    onRetry = {
                        viewModel.executeSearch(query = queryInput)
                    },
                )
            }
            is SearchLoadedUiState -> {
                if (current.searchResult.indexState.stale) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = KinetixPalette.CardSlate,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.search_stale_index_banner),
                            style = MaterialTheme.typography.labelMedium,
                            color = KinetixPalette.WarningAmber,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )
                }

                if (current.searchResult.categoryFacets.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        item {
                            FilterChip(
                                selected = current.selectedCategorySlug == null,
                                onClick = {
                                    viewModel.executeSearch(
                                        query = queryInput,
                                        categorySlug = null,
                                        sort = current.selectedSort,
                                    )
                                },
                                label = { Text(text = stringResource(R.string.home_category_all)) },
                            )
                        }
                        items(current.searchResult.categoryFacets) { facet ->
                            val slug = CategorySlug(facet.value)
                            FilterChip(
                                selected = current.selectedCategorySlug == slug,
                                onClick = {
                                    viewModel.executeSearch(
                                        query = queryInput,
                                        categorySlug = slug,
                                        sort = current.selectedSort,
                                    )
                                },
                                label = { Text(text = "${facet.value} (${facet.count})") },
                            )
                        }
                    }
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )
                }

                Text(
                    text = stringResource(
                        R.string.search_results_count,
                        current.searchResult.total,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = KinetixPalette.TextSecondary,
                )
                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(current.searchResult.hits) { hit ->
                        val formattedPrice = BigDecimal.valueOf(hit.priceMinor)
                            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                            .toPlainString()

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = KinetixPalette.CardSlate,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProduct(hit.sku) },
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Text(
                                    text = hit.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KinetixPalette.TextPrimary,
                                )
                                if (hit.categories.isNotEmpty()) {
                                    Spacer(
                                        modifier = Modifier.height(4.dp),
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.search_category_prefix,
                                            hit.categories.joinToString(", ") { it.value },
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KinetixPalette.CyanPulse,
                                    )
                                }
                                Spacer(
                                    modifier = Modifier.height(6.dp),
                                )
                                Text(
                                    text = "${hit.currency} $formattedPrice",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = KinetixPalette.ElectricVolt,
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.executeSearch(
                                query = current.query,
                                categorySlug = current.selectedCategorySlug,
                                sort = current.selectedSort,
                                offset = (current.currentOffset - current.pageLimit).coerceAtLeast(0),
                            )
                        },
                        enabled = current.currentOffset > 0,
                    ) {
                        Text(
                            text = stringResource(R.string.action_prev),
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.executeSearch(
                                query = current.query,
                                categorySlug = current.selectedCategorySlug,
                                sort = current.selectedSort,
                                offset = current.currentOffset + current.pageLimit,
                            )
                        },
                        enabled = current.currentOffset + current.pageLimit < current.searchResult.total,
                    ) {
                        Text(
                            text = stringResource(R.string.action_next),
                        )
                    }
                }
            }
        }
    }
}
