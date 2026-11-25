package com.kinetix.shop.state.profile

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.models.profile.ProfileMapStatusNotice
import com.kinetix.shop.models.profile.ResolvedAddress

data class ProfileLoadedUiState(
    val profile: DeliveryProfile?,
    val savedBanner: Boolean = false,
    val queuedOfflineBanner: Boolean = false,
    val errorFailure: Failure? = null,
    val selectedLatitude: Double? = profile?.latitude,
    val selectedLongitude: Double? = profile?.longitude,
    val isGeocodingMap: Boolean = false,
    val mapStatusNotice: ProfileMapStatusNotice? = null,
    val resolvedAddressFromMap: ResolvedAddress? = null,
) : ProfileUiState
