package com.kinetix.shop.state.profile

import com.kinetix.shop.models.profile.DeliveryProfile

data class ProfileSavingUiState(
    val currentProfile: DeliveryProfile?,
) : ProfileUiState
