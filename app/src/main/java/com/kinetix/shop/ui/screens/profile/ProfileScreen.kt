package com.kinetix.shop.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetix.shop.R
import com.kinetix.shop.state.profile.ProfileErrorUiState
import com.kinetix.shop.state.profile.ProfileLoadedUiState
import com.kinetix.shop.state.profile.ProfileLoadingUiState
import com.kinetix.shop.state.profile.ProfileSavingUiState
import com.kinetix.shop.ui.components.ErrorBannerBlock
import com.kinetix.shop.ui.components.FailureMessageMapper
import com.kinetix.shop.ui.components.LoadingIndicatorBlock
import com.kinetix.shop.ui.theme.KinetixPalette
import com.kinetix.shop.viewmodels.profile.ProfileViewModel

@Composable
fun ProfileScreen(
    email: String,
    accountId: Long?,
    principalId: String,
    viewModel: ProfileViewModel,
    onNavigateToTwoFactorSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var fullName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var streetAddress by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var postalCode by rememberSaveable { mutableStateOf("") }
    var initializedForProfileId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    val currentProfile = when (val current = state) {
        is ProfileLoadedUiState -> current.profile
        is ProfileSavingUiState -> current.currentProfile
        else -> null
    }

    LaunchedEffect(currentProfile) {
        if (currentProfile != null && initializedForProfileId != currentProfile.id) {
            fullName = currentProfile.fullName
            phoneNumber = currentProfile.phoneNumber
            streetAddress = currentProfile.streetAddress
            city = currentProfile.city
            postalCode = currentProfile.postalCode
            initializedForProfileId = currentProfile.id
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium,
            color = KinetixPalette.ElectricVolt,
        )

        when (val current = state) {
            is ProfileLoadingUiState -> {
                LoadingIndicatorBlock(
                    label = stringResource(R.string.profile_loading),
                )
            }
            is ProfileErrorUiState -> {
                ErrorBannerBlock(
                    failure = current.failure,
                    onRetry = { viewModel.loadProfile() },
                )
            }
            is ProfileLoadedUiState,
            is ProfileSavingUiState -> {
                val isSaving = current is ProfileSavingUiState
                val loadedState = current as? ProfileLoadedUiState
                val displayAccountId = currentProfile?.accountId?.value ?: accountId

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = KinetixPalette.CardSlate,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.titleMedium,
                            color = KinetixPalette.TextPrimary,
                        )
                        if (displayAccountId != null) {
                            Text(
                                text = stringResource(
                                    R.string.profile_account_format,
                                    displayAccountId,
                                    principalId,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = KinetixPalette.TextSecondary,
                            )
                        }
                        val lat = currentProfile?.latitude
                        val lng = currentProfile?.longitude
                        if (lat != null && lng != null) {
                            Text(
                                text = stringResource(
                                    R.string.profile_geocoded_format,
                                    lat,
                                    lng,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = KinetixPalette.CyanPulse,
                            )
                        }
                    }
                }

                if (currentProfile == null) {
                    Text(
                        text = stringResource(R.string.profile_not_created_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = KinetixPalette.TextSecondary,
                    )
                }

                if (loadedState?.savedBanner == true) {
                    Text(
                        text = stringResource(R.string.profile_saved_banner),
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixPalette.SuccessEmerald,
                    )
                }

                val errorFailure = loadedState?.errorFailure
                if (errorFailure != null) {
                    Text(
                        text = FailureMessageMapper.resolve(errorFailure),
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixPalette.DangerCoral,
                    )
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_full_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_phone)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = streetAddress,
                    onValueChange = { streetAddress = it },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_street)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_city)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_postal_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                Button(
                    onClick = {
                        viewModel.saveProfile(
                            fullNameInput = fullName,
                            phoneNumberInput = phoneNumber,
                            streetAddressInput = streetAddress,
                            cityInput = city,
                            postalCodeInput = postalCode,
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (isSaving) {
                            stringResource(R.string.profile_save_button_saving)
                        } else {
                            stringResource(R.string.profile_save_button_idle)
                        },
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToTwoFactorSettings,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.profile_manage_2fa_button),
                    )
                }

                OutlinedButton(
                    onClick = onSignOut,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.profile_sign_out_button),
                        color = KinetixPalette.DangerCoral,
                    )
                }
            }
        }
    }
}
