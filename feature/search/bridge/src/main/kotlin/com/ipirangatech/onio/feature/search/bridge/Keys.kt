package com.ipirangatech.onio.feature.search.bridge

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class SearchKey(val categorySlug: String? = null) : NavKey
