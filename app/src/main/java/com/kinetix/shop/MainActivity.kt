package com.kinetix.shop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kinetix.shop.core.config.AppConfigCheckResult
import com.kinetix.shop.core.config.AppConfigInvalid
import com.kinetix.shop.core.config.AppConfigValid
import com.kinetix.shop.repositories.auth.SessionRepository
import com.kinetix.shop.ui.screens.AppRootScreen
import com.kinetix.shop.ui.screens.ConfigErrorScreen
import com.kinetix.shop.ui.theme.KinetixTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var configCheckResult: AppConfigCheckResult

    @Inject
    lateinit var sessionRepository: dagger.Lazy<SessionRepository>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KinetixTheme {
                when (val result = configCheckResult) {
                    is AppConfigInvalid -> {
                        ConfigErrorScreen(
                            reasons = result.problems,
                        )
                    }
                    is AppConfigValid -> {
                        AppRootScreen(
                            sessionRepository = sessionRepository.get(),
                        )
                    }
                }
            }
        }
    }
}
