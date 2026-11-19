package com.kinetix.shop.viewmodels.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.repositories.search.SearchRepository
import com.kinetix.shop.state.search.SearchErrorUiState
import com.kinetix.shop.state.search.SearchIdleUiState
import com.kinetix.shop.state.search.SearchLoadedUiState
import com.kinetix.shop.state.search.SearchLoadingUiState
import com.kinetix.shop.state.search.SearchUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<SearchUiState>(SearchIdleUiState)
    val uiState: StateFlow<SearchUiState> = mutableState.asStateFlow()

    private var currentQuery: String = ""
    private var currentCategory: CategorySlug? = null
    private var currentSort: String? = null
    private var currentSuggestions: List<String> = emptyList()
    private val pageLimit: Int = NetworkPolicy.DEFAULT_PAGE_SIZE

    private var suggestJob: Job? = null
    private var searchJob: Job? = null

    fun updateQueryAndSuggest(
        query: String,
    ) {
        currentQuery = query
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(NetworkPolicy.SEARCH_DEBOUNCE_MILLIS)
            val suggestions = when (
                val suggestResult = searchRepository.suggest(
                    query = query,
                    limit = NetworkPolicy.DEFAULT_SUGGESTION_LIMIT,
                )
            ) {
                is Ok -> suggestResult.value
                is Err -> emptyList()
            }
            if (currentQuery != query) {
                return@launch
            }
            currentSuggestions = suggestions
            val loaded = mutableState.value as? SearchLoadedUiState
            if (loaded != null) {
                mutableState.value = loaded.copy(
                    query = currentQuery,
                    suggestions = suggestions,
                )
            }
        }
    }

    fun executeSearch(
        query: String = currentQuery,
        categorySlug: CategorySlug? = currentCategory,
        sort: String? = currentSort,
        offset: Int = 0,
    ) {
        if (query != currentQuery) {
            suggestJob?.cancel()
            currentSuggestions = emptyList()
        }
        currentQuery = query
        currentCategory = categorySlug
        currentSort = sort
        searchJob?.cancel()
        mutableState.value = SearchLoadingUiState
        searchJob = viewModelScope.launch {
            when (
                val result = searchRepository.search(
                    query = currentQuery,
                    categorySlug = currentCategory,
                    sort = currentSort,
                    limit = pageLimit,
                    offset = offset.coerceAtLeast(0),
                )
            ) {
                is Ok -> {
                    mutableState.value = SearchLoadedUiState(
                        query = currentQuery,
                        selectedCategorySlug = currentCategory,
                        selectedSort = currentSort,
                        suggestions = currentSuggestions,
                        searchResult = result.value,
                        currentOffset = offset.coerceAtLeast(0),
                        pageLimit = pageLimit,
                    )
                }
                is Err -> {
                    mutableState.value = SearchErrorUiState(
                        failure = result.failure,
                    )
                }
            }
        }
    }
}
