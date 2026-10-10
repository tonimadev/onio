package com.ipirangatech.onio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.CouponCatalog
import com.ipirangatech.onio.core.model.CouponRepository
import com.ipirangatech.onio.core.model.RewardsRepository
import com.ipirangatech.onio.core.model.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-wide display preferences (currency override) shared through a CompositionLocal. */
@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    cart: CartRepository,
    private val coupons: CouponRepository,
    private val rewards: RewardsRepository,
) : ViewModel() {
    /** First-run gift: a welcome coupon and the first free spin, granted when onboarding finishes. */
    fun onOnboardingFinished() {
        viewModelScope.launch {
            runCatching {
                coupons.grant(CouponCatalog.Welcome)
                rewards.refreshDaily()
            }
        }
    }

    /** null while DataStore is still loading; then whether onboarding is done. */
    val onboardingCompleted: StateFlow<Boolean?> = settings.settings
        .map<_, Boolean?> { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Total units in the cart, shown as a badge on the nav item. */
    val cartCount: StateFlow<Int> = cart.items
        .map { items -> items.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val currencyOverride: StateFlow<String?> = settings.settings
        .map { it.currencyOverride }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
