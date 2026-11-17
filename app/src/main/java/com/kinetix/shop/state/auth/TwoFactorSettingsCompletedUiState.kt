package com.kinetix.shop.state.auth

data class TwoFactorSettingsCompletedUiState(
    val enabled: Boolean,
) : TwoFactorSettingsUiState
