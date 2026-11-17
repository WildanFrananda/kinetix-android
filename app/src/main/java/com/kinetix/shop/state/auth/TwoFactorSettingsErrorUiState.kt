package com.kinetix.shop.state.auth

import com.kinetix.shop.core.failure.Failure

data class TwoFactorSettingsErrorUiState(
    val failure: Failure,
) : TwoFactorSettingsUiState
