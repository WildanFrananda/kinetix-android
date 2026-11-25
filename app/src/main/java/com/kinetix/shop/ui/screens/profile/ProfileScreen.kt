package com.kinetix.shop.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetix.shop.R
import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.state.profile.ProfileErrorUiState
import com.kinetix.shop.state.profile.ProfileLoadedUiState
import com.kinetix.shop.state.profile.ProfileLoadingUiState
import com.kinetix.shop.state.profile.ProfileSavingUiState
import com.kinetix.shop.ui.components.DeliveryMapPicker
import com.kinetix.shop.ui.components.ErrorBannerBlock
import com.kinetix.shop.ui.components.FailureMessageMapper
import com.kinetix.shop.ui.components.ProfileSkeleton
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
    val context = LocalContext.current

    var fullName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var streetAddress by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var postalCode by rememberSaveable { mutableStateOf("") }
    var initializedForProfileId by rememberSaveable { mutableStateOf<Long?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.onUseCurrentLocation()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val currentProfile = when (val current = state) {
        is ProfileLoadedUiState -> current.profile
        is ProfileSavingUiState -> current.currentProfile
        else -> null
    }

    val loadedForMap = state as? ProfileLoadedUiState
    val resolvedAddressFromMap = loadedForMap?.resolvedAddressFromMap

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

    LaunchedEffect(resolvedAddressFromMap) {
        if (resolvedAddressFromMap != null) {
            if (resolvedAddressFromMap.streetAddress.isNotBlank()) {
                streetAddress = resolvedAddressFromMap.streetAddress
            }
            if (resolvedAddressFromMap.city.isNotBlank()) {
                city = resolvedAddressFromMap.city
            }
            if (resolvedAddressFromMap.postalCode.isNotBlank()) {
                postalCode = resolvedAddressFromMap.postalCode
            }
            viewModel.consumeResolvedAddressFromMap()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        when (val current = state) {
            is ProfileLoadingUiState -> {
                ProfileSkeleton()
            }
            is ProfileErrorUiState -> {
                ErrorBannerBlock(
                    failure = current.failure,
                    onRetry = { viewModel.refreshProfile() },
                )
            }
            is ProfileLoadedUiState,
            is ProfileSavingUiState -> {
                val isSaving = current is ProfileSavingUiState
                val loadedState = current as? ProfileLoadedUiState
                val displayAccountId = currentProfile?.accountId?.value ?: accountId

                Card(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val initialsSource = fullName.trim().ifEmpty { email }
                        val initial = initialsSource.firstOrNull()?.uppercaseChar()?.toString() ?: "K"
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ),
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (displayAccountId != null) {
                                Text(
                                    text = stringResource(
                                        R.string.profile_account_format,
                                        displayAccountId,
                                        principalId,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KinetixPalette.TextMuted,
                                )
                            }
                            val displayLat = loadedState?.selectedLatitude ?: currentProfile?.latitude
                            val displayLng = loadedState?.selectedLongitude ?: currentProfile?.longitude
                            if (displayLat != null && displayLng != null) {
                                Text(
                                    text = stringResource(
                                        R.string.profile_geocoded_format,
                                        displayLat,
                                        displayLng,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KinetixPalette.TextSecondary,
                                )
                            }
                        }
                    }
                }

                if (currentProfile == null) {
                    Text(
                        text = stringResource(R.string.profile_not_created_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (loadedState?.savedBanner == true) {
                    Text(
                        text = stringResource(R.string.profile_saved_banner),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                if (loadedState?.queuedOfflineBanner == true) {
                    Text(
                        text = stringResource(R.string.profile_queued_offline_banner),
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixPalette.WarningAmber,
                    )
                } else if (currentProfile?.dataOrigin == DataOrigin.CACHE) {
                    Text(
                        text = stringResource(R.string.data_cached_banner),
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixPalette.WarningAmber,
                    )
                }

                val errorFailure = loadedState?.errorFailure
                if (errorFailure != null) {
                    Text(
                        text = FailureMessageMapper.resolve(errorFailure),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                DeliveryMapPicker(
                    latitude = loadedState?.selectedLatitude ?: currentProfile?.latitude,
                    longitude = loadedState?.selectedLongitude ?: currentProfile?.longitude,
                    isGeocoding = loadedState?.isGeocodingMap == true,
                    statusNotice = loadedState?.mapStatusNotice,
                    enabled = !isSaving,
                    onSelectCoordinates = { lat, lng ->
                        viewModel.onMapLocationSelected(lat, lng)
                    },
                    onRequestCurrentLocation = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            viewModel.onUseCurrentLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        }
                    },
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_full_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.PersonFullName },
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_phone)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.PhoneNumber },
                )
                OutlinedTextField(
                    value = streetAddress,
                    onValueChange = { updated ->
                        streetAddress = updated
                        viewModel.onAddressFormChanged(
                            streetAddress = updated,
                            city = city,
                            postalCode = postalCode,
                        )
                    },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_street)) },
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.AddressStreet },
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { updated ->
                        city = updated
                        viewModel.onAddressFormChanged(
                            streetAddress = streetAddress,
                            city = updated,
                            postalCode = postalCode,
                        )
                    },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_city)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.AddressLocality },
                )
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { updated ->
                        postalCode = updated
                        viewModel.onAddressFormChanged(
                            streetAddress = streetAddress,
                            city = city,
                            postalCode = updated,
                        )
                    },
                    enabled = !isSaving,
                    label = { Text(text = stringResource(R.string.profile_field_postal_code)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.PostalCode },
                )

                Spacer(
                    modifier = Modifier.height(6.dp),
                )

                Button(
                    shape = MaterialTheme.shapes.small,
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
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    onClick = onNavigateToTwoFactorSettings,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.profile_manage_2fa_button),
                    )
                }

                OutlinedButton(
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    onClick = onSignOut,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.profile_sign_out_button),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
