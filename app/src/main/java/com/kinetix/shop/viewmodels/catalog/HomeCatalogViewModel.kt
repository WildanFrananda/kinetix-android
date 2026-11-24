package com.kinetix.shop.viewmodels.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.repositories.catalog.CatalogRepository
import com.kinetix.shop.state.catalog.HomeCatalogErrorUiState
import com.kinetix.shop.state.catalog.HomeCatalogLoadedUiState
import com.kinetix.shop.state.catalog.HomeCatalogLoadingUiState
import com.kinetix.shop.state.catalog.HomeCatalogUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeCatalogViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<HomeCatalogUiState>(HomeCatalogLoadingUiState)
    val uiState: StateFlow<HomeCatalogUiState> = mutableState.asStateFlow()

    private var cachedCategories: List<Category> = emptyList()
    private var activeCategorySlug: CategorySlug? = null
    private var loadJob: Job? = null

    init {
        loadPage(
            categorySlug = null,
            page = 1,
        )
    }

    fun loadInitialCatalog() {
        if (loadJob?.isActive == true || mutableState.value is HomeCatalogLoadedUiState) {
            return
        }
        loadPage(
            categorySlug = activeCategorySlug,
            page = 1,
        )
    }

    fun refresh() {
        loadPage(
            categorySlug = activeCategorySlug,
            page = 1,
        )
    }

    fun selectCategory(
        categorySlug: CategorySlug?,
    ) {
        activeCategorySlug = categorySlug
        loadPage(
            categorySlug = categorySlug,
            page = 1,
        )
    }

    fun goToPage(
        page: Int,
    ) {
        loadPage(
            categorySlug = activeCategorySlug,
            page = page.coerceAtLeast(1),
        )
    }

    private fun loadPage(
        categorySlug: CategorySlug?,
        page: Int,
    ) {
        loadJob?.cancel()
        mutableState.value = HomeCatalogLoadingUiState
        loadJob = viewModelScope.launch {
            val categoriesDeferred = if (cachedCategories.isEmpty()) {
                async { catalogRepository.listCategories() }
            } else {
                null
            }
            val productsDeferred = async {
                catalogRepository.listProducts(
                    categorySlug = categorySlug,
                    page = page,
                )
            }

            var categoriesUnavailable = false
            if (categoriesDeferred != null) {
                when (val catResult = categoriesDeferred.await()) {
                    is Ok -> {
                        cachedCategories = catResult.value
                    }
                    is Err -> {
                        categoriesUnavailable = true
                    }
                }
            }

            when (val prodResult = productsDeferred.await()) {
                is Ok -> {
                    mutableState.value = HomeCatalogLoadedUiState(
                        categories = cachedCategories,
                        selectedCategorySlug = categorySlug,
                        productPage = prodResult.value,
                        categoriesUnavailable = categoriesUnavailable,
                    )
                }
                is Err -> {
                    mutableState.value = HomeCatalogErrorUiState(
                        failure = prodResult.failure,
                    )
                }
            }
        }
    }
}
