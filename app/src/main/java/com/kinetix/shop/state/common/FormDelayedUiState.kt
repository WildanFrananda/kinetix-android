package com.kinetix.shop.state.common

data class FormDelayedUiState(
    val secondsRemaining: Int,
) : AsyncFormUiState
