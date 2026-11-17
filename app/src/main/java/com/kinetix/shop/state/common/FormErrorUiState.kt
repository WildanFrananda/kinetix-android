package com.kinetix.shop.state.common

import com.kinetix.shop.core.failure.Failure

data class FormErrorUiState(
    val failure: Failure,
) : AsyncFormUiState
