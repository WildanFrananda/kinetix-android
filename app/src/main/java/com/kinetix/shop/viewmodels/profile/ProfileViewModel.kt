package com.kinetix.shop.viewmodels.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.failure.ClientValidationFailure
import com.kinetix.shop.core.failure.ClientValidationReason
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.models.profile.ProfileMapStatusNotice
import com.kinetix.shop.models.profile.ProfileUpdateConfirmed
import com.kinetix.shop.models.profile.ProfileUpdateQueued
import com.kinetix.shop.models.profile.UpdateDeliveryProfileInput
import com.kinetix.shop.repositories.profile.ProfileRepository
import com.kinetix.shop.services.location.AddressGeocoder
import com.kinetix.shop.services.location.DeviceLocationProvider
import com.kinetix.shop.state.profile.ProfileErrorUiState
import com.kinetix.shop.state.profile.ProfileLoadedUiState
import com.kinetix.shop.state.profile.ProfileLoadingUiState
import com.kinetix.shop.state.profile.ProfileSavingUiState
import com.kinetix.shop.state.profile.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val addressGeocoder: AddressGeocoder,
    private val deviceLocationProvider: DeviceLocationProvider,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ProfileUiState>(ProfileLoadingUiState)
    val uiState: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var geocodeJob: Job? = null
    private var operationGeneration: Long = 0L
    private var geocodedAddressSignature: String? = null

    init {
        startLoad()
    }

    fun loadProfile() {
        if (loadJob?.isActive == true || mutableState.value is ProfileLoadedUiState) {
            return
        }
        startLoad()
    }

    fun refreshProfile() {
        startLoad()
    }

    fun onMapLocationSelected(
        latitude: Double,
        longitude: Double,
    ) {
        val currentState = mutableState.value as? ProfileLoadedUiState ?: return
        geocodeJob?.cancel()
        mutableState.value = currentState.copy(
            selectedLatitude = latitude,
            selectedLongitude = longitude,
            isGeocodingMap = true,
            mapStatusNotice = null,
        )
        geocodeJob = viewModelScope.launch {
            val resolved = addressGeocoder.reverseGeocode(
                latitude = latitude,
                longitude = longitude,
            )
            val latest = mutableState.value as? ProfileLoadedUiState ?: return@launch
            geocodedAddressSignature = if (resolved != null) {
                addressSignature(resolved.streetAddress, resolved.city, resolved.postalCode)
            } else {
                null
            }
            mutableState.value = latest.copy(
                selectedLatitude = latitude,
                selectedLongitude = longitude,
                isGeocodingMap = false,
                resolvedAddressFromMap = resolved,
                mapStatusNotice = if (resolved != null) {
                    ProfileMapStatusNotice.ADDRESS_UPDATED_FROM_PIN
                } else {
                    latest.mapStatusNotice
                },
            )
        }
    }

    fun onAddressFormChanged(
        streetAddress: String,
        city: String,
        postalCode: String,
    ) {
        if (mutableState.value !is ProfileLoadedUiState) {
            return
        }
        val signature = addressSignature(streetAddress, city, postalCode)
        if (signature != geocodedAddressSignature) {
            geocodedAddressSignature = null
        }
        geocodeJob?.cancel()
        val hasQuery = streetAddress.isNotBlank() || city.isNotBlank() || postalCode.isNotBlank()
        if (!hasQuery) {
            return
        }
        geocodeJob = viewModelScope.launch {
            delay(ADDRESS_GEOCODE_DEBOUNCE_MS.milliseconds)
            val currentBefore = mutableState.value as? ProfileLoadedUiState ?: return@launch
            mutableState.value = currentBefore.copy(isGeocodingMap = true)
            val coordinates = addressGeocoder.forwardGeocode(
                streetAddress = streetAddress,
                city = city,
                postalCode = postalCode,
            )
            val latest = mutableState.value as? ProfileLoadedUiState ?: return@launch
            mutableState.value = if (coordinates != null) {
                geocodedAddressSignature = signature
                latest.copy(
                    selectedLatitude = coordinates.latitude,
                    selectedLongitude = coordinates.longitude,
                    isGeocodingMap = false,
                    mapStatusNotice = ProfileMapStatusNotice.PIN_UPDATED_FROM_ADDRESS,
                )
            } else {
                geocodedAddressSignature = null
                latest.copy(isGeocodingMap = false)
            }
        }
    }

    fun onUseCurrentLocation() {
        val currentState = mutableState.value as? ProfileLoadedUiState ?: return
        geocodeJob?.cancel()
        mutableState.value = currentState.copy(
            isGeocodingMap = true,
            mapStatusNotice = null,
        )
        geocodeJob = viewModelScope.launch {
            val coordinates = deviceLocationProvider.getCurrentLocation()
            val latest = mutableState.value as? ProfileLoadedUiState ?: return@launch
            if (coordinates == null) {
                mutableState.value = latest.copy(
                    isGeocodingMap = false,
                    mapStatusNotice = ProfileMapStatusNotice.LOCATION_UNAVAILABLE,
                )
                return@launch
            }
            onMapLocationSelected(
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
            )
        }
    }

    fun onLocationPermissionDenied() {
        val currentState = mutableState.value as? ProfileLoadedUiState ?: return
        mutableState.value = currentState.copy(
            mapStatusNotice = ProfileMapStatusNotice.PERMISSION_DENIED,
        )
    }

    fun consumeResolvedAddressFromMap() {
        val currentState = mutableState.value as? ProfileLoadedUiState ?: return
        if (currentState.resolvedAddressFromMap != null) {
            mutableState.value = currentState.copy(resolvedAddressFromMap = null)
        }
    }

    fun saveProfile(
        fullNameInput: String,
        phoneNumberInput: String,
        streetAddressInput: String,
        cityInput: String,
        postalCodeInput: String,
    ) {
        val currentState = mutableState.value
        if (currentState !is ProfileLoadedUiState) {
            return
        }
        val existingProfile = currentState.profile

        val trimmedName = fullNameInput.trim()
        val trimmedPhone = phoneNumberInput.trim()
        val trimmedStreet = streetAddressInput.trim()
        val trimmedCity = cityInput.trim()
        val trimmedPostal = postalCodeInput.trim()

        if (
            trimmedName.isEmpty() ||
            trimmedPhone.isEmpty() ||
            trimmedStreet.isEmpty() ||
            trimmedCity.isEmpty() ||
            trimmedPostal.isEmpty()
        ) {
            mutableState.value = currentState.copy(
                errorFailure = ClientValidationFailure(
                    ClientValidationReason.PROFILE_FIELDS_REQUIRED,
                ),
            )
            return
        }

        val submittedSignature = addressSignature(trimmedStreet, trimmedCity, trimmedPostal)
        val selectedLat = currentState.selectedLatitude
        val selectedLng = currentState.selectedLongitude
        val hasMatchingCoordinates = selectedLat != null &&
            selectedLng != null &&
            geocodedAddressSignature == submittedSignature
        val coordinatesChanged = hasMatchingCoordinates &&
            (selectedLat != existingProfile?.latitude || selectedLng != existingProfile.longitude)

        val input = if (existingProfile == null) {
            UpdateDeliveryProfileInput(
                fullName = trimmedName,
                phoneNumber = trimmedPhone,
                streetAddress = trimmedStreet,
                city = trimmedCity,
                postalCode = trimmedPostal,
                latitude = if (hasMatchingCoordinates) selectedLat else null,
                longitude = if (hasMatchingCoordinates) selectedLng else null,
            )
        } else {
            UpdateDeliveryProfileInput(
                fullName = trimmedName.takeIf { it != existingProfile.fullName },
                phoneNumber = trimmedPhone.takeIf { it != existingProfile.phoneNumber },
                streetAddress = trimmedStreet.takeIf { it != existingProfile.streetAddress },
                city = trimmedCity.takeIf { it != existingProfile.city },
                postalCode = trimmedPostal.takeIf { it != existingProfile.postalCode },
                latitude = if (coordinatesChanged) selectedLat else null,
                longitude = if (coordinatesChanged) selectedLng else null,
            )
        }
        if (!input.hasChanges()) {
            return
        }

        val generation = ++operationGeneration
        mutableState.value = ProfileSavingUiState(existingProfile)
        viewModelScope.launch {
            when (val result = profileRepository.updateProfile(input)) {
                is Ok -> {
                    if (generation != operationGeneration) {
                        return@launch
                    }
                    when (val outcome = result.value) {
                        is ProfileUpdateConfirmed -> {
                            geocodedAddressSignature = if (
                                outcome.profile.latitude != null &&
                                outcome.profile.longitude != null
                            ) {
                                addressSignature(
                                    outcome.profile.streetAddress,
                                    outcome.profile.city,
                                    outcome.profile.postalCode,
                                )
                            } else {
                                null
                            }
                            mutableState.value = ProfileLoadedUiState(
                                profile = outcome.profile,
                                savedBanner = true,
                                queuedOfflineBanner = false,
                                selectedLatitude = outcome.profile.latitude,
                                selectedLongitude = outcome.profile.longitude,
                            )
                        }
                        is ProfileUpdateQueued -> {
                            geocodedAddressSignature = if (
                                outcome.profile.latitude != null &&
                                outcome.profile.longitude != null
                            ) {
                                addressSignature(
                                    outcome.profile.streetAddress,
                                    outcome.profile.city,
                                    outcome.profile.postalCode,
                                )
                            } else {
                                null
                            }
                            mutableState.value = ProfileLoadedUiState(
                                profile = outcome.profile,
                                savedBanner = false,
                                queuedOfflineBanner = true,
                                selectedLatitude = outcome.profile.latitude,
                                selectedLongitude = outcome.profile.longitude,
                            )
                        }
                    }
                }
                is Err -> {
                    if (generation != operationGeneration) {
                        return@launch
                    }
                    mutableState.value = currentState.copy(
                        errorFailure = result.failure,
                    )
                }
            }
        }
    }

    private fun startLoad() {
        loadJob?.cancel()
        val generation = ++operationGeneration
        loadJob = viewModelScope.launch {
            val cached = profileRepository.getCachedProfile()
            if (generation != operationGeneration) {
                return@launch
            }
            if (cached != null) {
                geocodedAddressSignature = if (cached.latitude != null && cached.longitude != null) {
                    addressSignature(cached.streetAddress, cached.city, cached.postalCode)
                } else {
                    null
                }
                mutableState.value = ProfileLoadedUiState(
                    profile = cached,
                    queuedOfflineBanner = !cached.isConfirmed,
                    selectedLatitude = cached.latitude,
                    selectedLongitude = cached.longitude,
                )
            } else {
                mutableState.value = ProfileLoadingUiState
            }

            when (val result = profileRepository.getProfile()) {
                is Ok -> {
                    if (generation != operationGeneration) {
                        return@launch
                    }
                    geocodedAddressSignature = if (
                        result.value.latitude != null &&
                        result.value.longitude != null
                    ) {
                        addressSignature(
                            result.value.streetAddress,
                            result.value.city,
                            result.value.postalCode,
                        )
                    } else {
                        null
                    }
                    mutableState.value = ProfileLoadedUiState(
                        profile = result.value,
                        queuedOfflineBanner = !result.value.isConfirmed,
                        selectedLatitude = result.value.latitude,
                        selectedLongitude = result.value.longitude,
                    )
                }
                is Err -> {
                    if (generation != operationGeneration) {
                        return@launch
                    }
                    if (result.failure is NotFoundFailure) {
                        geocodedAddressSignature = null
                        mutableState.value = ProfileLoadedUiState(profile = null)
                    } else if (cached == null) {
                        mutableState.value = ProfileErrorUiState(failure = result.failure)
                    }
                }
            }
        }
    }

    private fun addressSignature(
        streetAddress: String,
        city: String,
        postalCode: String,
    ): String {
        return "${streetAddress.trim()}|${city.trim()}|${postalCode.trim()}"
    }

    companion object {
        private const val ADDRESS_GEOCODE_DEBOUNCE_MS = 550L
    }
}
