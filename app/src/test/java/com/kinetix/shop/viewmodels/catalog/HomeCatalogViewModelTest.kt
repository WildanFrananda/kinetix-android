package com.kinetix.shop.viewmodels.catalog

import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeCatalogRepository
import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.catalog.ProductPage
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.state.catalog.HomeCatalogLoadedUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeCatalogViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsCategoriesAndProductsAndFiltersByCategory() = runTest(testDispatcher) {
        val electronicsSlug = CategorySlug("electronics")
        val repository = FakeCatalogRepository().apply {
            categoriesResult = Ok(
                listOf(
                    Category(
                        id = 1L,
                        name = "Electronics",
                        slug = electronicsSlug,
                    )
                )
            )
            productsResult = Ok(
                ProductPage(
                    totalCount = 0,
                    page = 1,
                    pageSize = 20,
                    items = emptyList(),
                )
            )
        }
        val viewModel = HomeCatalogViewModel(repository)

        viewModel.loadInitialCatalog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is HomeCatalogLoadedUiState)

        viewModel.selectCategory(electronicsSlug)
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as HomeCatalogLoadedUiState
        assertEquals(electronicsSlug, loaded.selectedCategorySlug)
        assertEquals(electronicsSlug, repository.lastRequestedCategory)
    }

    @Test
    fun flagsCategoriesUnavailableWhenCategoryLoadFailsButProductsSucceed() = runTest(testDispatcher) {
        val repository = FakeCatalogRepository().apply {
            categoriesResult = Err(
                UnavailableFailure(
                    code = "UPSTREAM_UNAVAILABLE",
                    retryAfterSeconds = null,
                )
            )
            productsResult = Ok(
                ProductPage(
                    totalCount = 0,
                    page = 1,
                    pageSize = 20,
                    items = emptyList(),
                )
            )
        }
        val viewModel = HomeCatalogViewModel(repository)

        viewModel.loadInitialCatalog()
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as HomeCatalogLoadedUiState
        assertTrue(loaded.categoriesUnavailable)
    }
}
