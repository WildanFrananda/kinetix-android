package com.kinetix.shop.viewmodels.search

import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeSearchRepository
import com.kinetix.shop.models.search.ProductSearchResult
import com.kinetix.shop.models.search.SearchIndexState
import com.kinetix.shop.state.search.SearchLoadedUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

    @Test
    fun debouncesRapidSuggestionInputsBeforeQueryingRepository() = runTest(testDispatcher) {
        val repository = FakeSearchRepository().apply {
            suggestResult = Ok(listOf("keyboard", "keycap"))
            searchResult = Ok(
                ProductSearchResult(
                    total = 0,
                    hits = emptyList(),
                    categoryFacets = emptyList(),
                    indexState = SearchIndexState(
                        indexedThroughEpochMillis = null,
                        ageSeconds = null,
                        stale = false,
                    ),
                )
            )
        }
        val viewModel = SearchViewModel(repository)

        viewModel.executeSearch(query = "k")
        advanceUntilIdle()

        viewModel.updateQueryAndSuggest("ke")
        advanceTimeBy(NetworkPolicy.SEARCH_DEBOUNCE_MILLIS / 2)
        assertTrue(repository.suggestQueries.isEmpty())

        viewModel.updateQueryAndSuggest("key")
        advanceTimeBy(NetworkPolicy.SEARCH_DEBOUNCE_MILLIS - 1L)
        assertTrue(repository.suggestQueries.isEmpty())

        advanceTimeBy(50L)
        assertEquals(listOf("key"), repository.suggestQueries)

        val loaded = viewModel.uiState.value as SearchLoadedUiState
        assertEquals("key", loaded.query)
        assertEquals(listOf("keyboard", "keycap"), loaded.suggestions)
    }

    @Test
    fun executeSearchWithDifferentQueryCancelsPendingSuggestions() = runTest(testDispatcher) {
        val repository = FakeSearchRepository().apply {
            suggestResult = Ok(listOf("ke-suggestion"))
            searchResult = Ok(
                ProductSearchResult(
                    total = 0,
                    hits = emptyList(),
                    categoryFacets = emptyList(),
                    indexState = SearchIndexState(
                        indexedThroughEpochMillis = null,
                        ageSeconds = null,
                        stale = false,
                    ),
                )
            )
        }
        val viewModel = SearchViewModel(repository)

        viewModel.updateQueryAndSuggest("ke")
        advanceTimeBy(NetworkPolicy.SEARCH_DEBOUNCE_MILLIS / 2)
        viewModel.executeSearch(query = "keyboard")
        advanceUntilIdle()

        assertTrue(repository.suggestQueries.isEmpty())
        val loaded = viewModel.uiState.value as SearchLoadedUiState
        assertEquals("keyboard", loaded.query)
        assertTrue(loaded.suggestions.isEmpty())
    }
}
