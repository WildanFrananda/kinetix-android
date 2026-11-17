package com.kinetix.shop.viewmodels.search

import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeSearchRepository
import com.kinetix.shop.models.search.ProductSearchResult
import com.kinetix.shop.models.search.SearchIndexState
import com.kinetix.shop.state.search.SearchLoadedUiState
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
class SearchViewModelTest {
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
    fun executesSearchWithSortAndPreservesStaleIndexState() = runTest(testDispatcher) {
        val repository = FakeSearchRepository().apply {
            searchResult = Ok(
                ProductSearchResult(
                    total = 0,
                    hits = emptyList(),
                    categoryFacets = emptyList(),
                    indexState = SearchIndexState(
                        indexedThroughEpochMillis = 1_700_000_000_000L,
                        ageSeconds = 95L,
                        stale = true,
                    ),
                )
            )
        }
        val viewModel = SearchViewModel(repository)

        viewModel.executeSearch(
            query = "keyboard",
            sort = "price_asc",
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchLoadedUiState)
        val loaded = state as SearchLoadedUiState
        assertEquals("keyboard", loaded.query)
        assertEquals("price_asc", loaded.selectedSort)
        assertTrue(loaded.searchResult.indexState.stale)
    }
}
