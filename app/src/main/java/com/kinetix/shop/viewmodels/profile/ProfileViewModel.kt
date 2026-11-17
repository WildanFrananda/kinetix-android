package com.kinetix.shop.viewmodels.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.core.failure.ValidationFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.models.profile.UpdateDeliveryProfileInput
import com.kinetix.shop.repositories.profile.ProfileRepository
import com.kinetix.shop.state.profile.ProfileErrorUiState
import com.kinetix.shop.state.profile.ProfileLoadedUiState
import com.kinetix.shop.state.profile.ProfileLoadingUiState
import com.kinetix.shop.state.profile.ProfileSavingUiState
import com.kinetix.shop.state.profile.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

    fun loadProfile() {
        mutableState.value = ProfileLoadingUiState
        viewModelScope.launch {
            when (val result = profileRepository.getProfile()) {
                is Ok -> {
                    mutableState.value = ProfileLoadedUiState(profile = result.value)
                }
                is Err -> {
                    mutableState.value = ProfileErrorUiState(failure = result.failure)
                }
            }
        }
    }

    fun saveProfile(
        fullNameInput: String,
        phoneNumberInput: String,
        streetAddressInput: String,
        cityInput: String,
        postalCodeInput: String,
    ) {
        val currentLoaded = when (val state = mutableState.value) {
            is ProfileLoadedUiState -> state.profile
            is ProfileSavingUiState -> return
            else -> null
        } ?: return

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
                profile = currentLoaded,
                errorFailure = ValidationFailure(
                    code = "PROFILE_FIELDS_REQUIRED",
                    detail = "",
                    field = "profile",
                ),
            )
            return
        }

        val input = UpdateDeliveryProfileInput(
            fullName = trimmedName.takeIf { it != currentLoaded.fullName },
            phoneNumber = trimmedPhone.takeIf { it != currentLoaded.phoneNumber },
            streetAddress = trimmedStreet.takeIf { it != currentLoaded.streetAddress },
            city = trimmedCity.takeIf { it != currentLoaded.city },
            postalCode = trimmedPostal.takeIf { it != currentLoaded.postalCode },
        )
        if (!input.hasChanges()) {
            return
        }

        mutableState.value = ProfileSavingUiState(currentLoaded)
        viewModelScope.launch {
            when (val result = profileRepository.updateProfile(input)) {
                is Ok -> {
                    mutableState.value = ProfileLoadedUiState(
                        profile = result.value,
                        savedBanner = true,
                    )
                }
                is Err -> {
                    mutableState.value = ProfileLoadedUiState(
                        profile = currentLoaded,
                        errorFailure = result.failure,
                    )
                }
            }
        }
    }
}
