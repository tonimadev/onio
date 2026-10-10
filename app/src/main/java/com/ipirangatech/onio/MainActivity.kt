package com.ipirangatech.onio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.ui.component.ProvideCurrency
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import com.ipirangatech.onio.navigation.AppRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currency by mainViewModel.currencyOverride.collectAsStateWithLifecycle()
            val onboardingCompleted by mainViewModel.onboardingCompleted.collectAsStateWithLifecycle()
            OnioTheme {
                ProvideCurrency(currency) {
                    // Wait for DataStore so returning users never see a flash of onboarding.
                    onboardingCompleted?.let { completed ->
                        AppRoot(startWithOnboarding = !completed)
                    }
                }
            }
        }
    }
}
