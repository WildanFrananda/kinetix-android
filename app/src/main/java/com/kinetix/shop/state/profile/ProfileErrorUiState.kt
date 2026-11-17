package com.kinetix.shop.state.profile

import com.kinetix.shop.core.failure.Failure

data class ProfileErrorUiState(
    val failure: Failure,
) : ProfileUiState
