package com.ipirangatech.onio.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ipirangatech.onio.MainViewModel
import com.ipirangatech.onio.feature.onboarding.bridge.OnboardingKey
import com.ipirangatech.onio.feature.onboarding.impl.OnboardingRoute
import com.ipirangatech.onio.feature.onboarding.impl.OnboardingViewModel
import kotlinx.serialization.Serializable

/** Root destination holding the tabbed store; lives in :app because it is the composition root. */
@Serializable
data object MainKey : NavKey

/** Top-level flow: onboarding (first launch only) and then the main tabbed experience. */
@Composable
fun AppRoot(startWithOnboarding: Boolean, modifier: Modifier = Modifier) {
    val root = rememberNavBackStack(if (startWithOnboarding) OnboardingKey else MainKey)
    // Orders are cancelled in the background; the notification needs permission on Android 13+.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val mainViewModel = hiltViewModel<MainViewModel>()
    val cartCount by mainViewModel.cartCount.collectAsStateWithLifecycle()

    NavDisplay(
        backStack = root,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        ),
        onBack = { root.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<OnboardingKey> {
                OnboardingRoute(
                    viewModel = hiltViewModel<OnboardingViewModel>(),
                    onFinished = {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        root.add(MainKey)
                        root.removeAll { it == OnboardingKey }
                    },
                )
            }
            entry<MainKey> { OnioNavHost(cartCount = cartCount) }
        },
    )
}
