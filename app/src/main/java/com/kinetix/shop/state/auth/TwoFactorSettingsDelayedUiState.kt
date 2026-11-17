package com.kinetix.shop.state.auth

data class TwoFactorSettingsDelayedUiState(
    val secondsRemaining: Int,
) : TwoFactorSettingsUiState
