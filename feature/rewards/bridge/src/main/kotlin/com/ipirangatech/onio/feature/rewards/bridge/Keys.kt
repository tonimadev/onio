package com.ipirangatech.onio.feature.rewards.bridge

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Hub: coin balance, daily check-in, lucky wheel entry and the coupon wallet. */
@Serializable
data object RewardsKey : NavKey

@Serializable
data object WheelKey : NavKey

/** Celebration shown right after paying; carries what the purchase just earned. */
@Serializable
data class OrderPlacedKey(val orderId: Long, val coins: Int, val spins: Int) : NavKey
