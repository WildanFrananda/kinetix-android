package com.kinetix.shop.state.catalog

import com.kinetix.shop.core.failure.Failure

data class HomeCatalogErrorUiState(
    val failure: Failure,
) : HomeCatalogUiState
