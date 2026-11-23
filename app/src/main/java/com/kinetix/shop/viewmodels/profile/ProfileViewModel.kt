package com.kinetix.shop.viewmodels.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.failure.ClientValidationFailure
import com.kinetix.shop.core.failure.ClientValidationReason
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.models.profile.ProfileUpdateConfirmed
import com.kinetix.shop.models.profile.ProfileUpdateQueued
import com.kinetix.shop.models.profile.UpdateDeliveryProfileInput
import com.kinetix.shop.repositories.profile.ProfileRepository
import com.kinetix.shop.state.profile.ProfileErrorUiState
import com.kinetix.shop.state.profile.ProfileLoadedUiState
import com.kinetix.shop.state.profile.ProfileLoadingUiState
import com.kinetix.shop.state.profile.ProfileSavingUiState
import com.kinetix.shop.state.profile.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ProfileUiState>(ProfileLoadingUiState)
    val uiState: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

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
            mutableState.value = ProfileLoadedUiState(
                profile = existingProfile,
                errorFailure = ClientValidationFailure(
                    ClientValidationReason.PROFILE_FIELDS_REQUIRED,
                ),
            )
            return
        }

        val input = if (existingProfile == null) {
            UpdateDeliveryProfileInput(
                fullName = trimmedName,
                phoneNumber = trimmedPhone,
                streetAddress = trimmedStreet,
                city = trimmedCity,
                postalCode = trimmedPostal,
            )
        } else {
            UpdateDeliveryProfileInput(
                fullName = trimmedName.takeIf { it != existingProfile.fullName },
                phoneNumber = trimmedPhone.takeIf { it != existingProfile.phoneNumber },
                streetAddress = trimmedStreet.takeIf { it != existingProfile.streetAddress },
                city = trimmedCity.takeIf { it != existingProfile.city },
                postalCode = trimmedPostal.takeIf { it != existingProfile.postalCode },
            )
        }
        if (!input.hasChanges()) {
            return
        }

        mutableState.value = ProfileSavingUiState(existingProfile)
        viewModelScope.launch {
            when (val result = profileRepository.updateProfile(input)) {
                is Ok -> {
                    when (val outcome = result.value) {
                        is ProfileUpdateConfirmed -> {
                            mutableState.value = ProfileLoadedUiState(
                                profile = outcome.profile,
                                savedBanner = true,
                                queuedOfflineBanner = false,
                            )
                        }
                        is ProfileUpdateQueued -> {
                            mutableState.value = ProfileLoadedUiState(
                                profile = outcome.profile,
                                savedBanner = false,
                                queuedOfflineBanner = true,
                            )
                        }
                    }
                }
                is Err -> {
                    mutableState.value = ProfileLoadedUiState(
                        profile = existingProfile,
                        errorFailure = result.failure,
                    )
                }
            }
        }
    }

    private fun startLoad() {
        loadJob?.cancel()
        mutableState.value = ProfileLoadingUiState
        loadJob = viewModelScope.launch {
            when (val result = profileRepository.getProfile()) {
                is Ok -> {
                    mutableState.value = ProfileLoadedUiState(
                        profile = result.value,
                        queuedOfflineBanner = !result.value.isConfirmed,
                    )
                }
                is Err -> {
                    if (result.failure is NotFoundFailure) {
                        mutableState.value = ProfileLoadedUiState(profile = null)
                    } else {
                        mutableState.value = ProfileErrorUiState(failure = result.failure)
                    }
                }
            }
        }
    }
}
