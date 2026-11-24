package com.kinetix.shop

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kinetix.shop.core.config.AppConfigCheckResult
import com.kinetix.shop.core.config.AppConfigInvalid
import com.kinetix.shop.core.config.AppConfigValid
import com.kinetix.shop.ui.screens.config.ConfigErrorScreen
import com.kinetix.shop.ui.screens.root.AppRootScreen
import com.kinetix.shop.ui.theme.KinetixTheme
import com.kinetix.shop.viewmodels.root.AppRootViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var configCheckResult: AppConfigCheckResult

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            KinetixTheme {
                when (val result = configCheckResult) {
                    is AppConfigInvalid -> {
                        ConfigErrorScreen(
                            reasons = result.problems,
                        )
                    }
                    is AppConfigValid -> {
                        val rootViewModel: AppRootViewModel = hiltViewModel()
                        AppRootScreen(
                            viewModel = rootViewModel,
                        )
                    }
                }
            }
        }
    }
}
