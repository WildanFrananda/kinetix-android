package com.kinetix.shop.state.auth

import com.kinetix.shop.models.auth.TwoFactorSetupSecret

data class TwoFactorSettingsSecretReadyUiState(
    val secret: TwoFactorSetupSecret,
) : TwoFactorSettingsUiState
