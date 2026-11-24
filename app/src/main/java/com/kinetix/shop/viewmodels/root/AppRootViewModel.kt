package com.kinetix.shop.viewmodels.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetix.shop.BuildConfig
import com.kinetix.shop.models.auth.SessionState
import com.kinetix.shop.repositories.auth.SessionRepository
import com.kinetix.shop.services.config.FeatureFlagProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val featureFlagProvider: FeatureFlagProvider,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionRepository.sessionState

    private val mutableForceUpdateRequired = MutableStateFlow(
        featureFlagProvider.isForceUpdateRequired(BuildConfig.VERSION_CODE)
    )
    val forceUpdateRequired: StateFlow<Boolean> = mutableForceUpdateRequired.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            sessionRepository.restoreSession()
        }
        refreshFlags()
    }

    fun onAppForegrounded() {
        refreshFlags()
    }

    fun signOut() {
        viewModelScope.launch {
            sessionRepository.signOut()
        }
    }

    private fun refreshFlags() {
        if (refreshJob?.isActive == true) {
            return
        }
        refreshJob = viewModelScope.launch {
            featureFlagProvider.refreshFlags()
            mutableForceUpdateRequired.value = featureFlagProvider.isForceUpdateRequired(
                BuildConfig.VERSION_CODE,
            )
        }
    }
}
