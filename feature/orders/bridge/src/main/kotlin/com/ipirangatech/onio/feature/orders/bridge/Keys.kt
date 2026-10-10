package com.ipirangatech.onio.feature.orders.bridge

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object OrdersKey : NavKey

@Serializable
data class OrderDetailKey(val orderId: Long) : NavKey
