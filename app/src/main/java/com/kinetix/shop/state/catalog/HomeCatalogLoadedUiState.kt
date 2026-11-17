package com.kinetix.shop.state.catalog

import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.catalog.ProductPage
import com.kinetix.shop.models.ids.CategorySlug

data class HomeCatalogLoadedUiState(
    val categories: List<Category>,
    val selectedCategorySlug: CategorySlug?,
    val productPage: ProductPage,
    val categoriesUnavailable: Boolean = false,
) : HomeCatalogUiState
