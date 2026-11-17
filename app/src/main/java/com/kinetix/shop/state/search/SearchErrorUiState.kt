package com.kinetix.shop.state.search

import com.kinetix.shop.core.failure.Failure

data class SearchErrorUiState(
    val failure: Failure,
) : SearchUiState
