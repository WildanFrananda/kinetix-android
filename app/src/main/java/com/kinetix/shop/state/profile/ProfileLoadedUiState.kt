package com.kinetix.shop.state.profile

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.models.profile.DeliveryProfile

data class ProfileLoadedUiState(
    val profile: DeliveryProfile?,
    val savedBanner: Boolean = false,
    val errorFailure: Failure? = null,
) : ProfileUiState
