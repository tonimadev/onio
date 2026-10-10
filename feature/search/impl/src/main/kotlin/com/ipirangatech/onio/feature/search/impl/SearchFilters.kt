package com.ipirangatech.onio.feature.search.impl

import androidx.compose.runtime.Immutable

enum class SortOption { RELEVANCE, PRICE_ASC, PRICE_DESC, DISCOUNT, RATING }

/** Everything the user can narrow or reorder results with, applied locally to the loaded page. */
@Immutable
data class SearchFilters(
    val sort: SortOption = SortOption.RELEVANCE,
    val onSaleOnly: Boolean = false,
    val topRatedOnly: Boolean = false,
    /** Inclusive price bounds in USD cents; null means "no limit". */
    val minPriceCents: Long? = null,
    val maxPriceCents: Long? = null,
) {
    val priceRangeActive: Boolean get() = minPriceCents != null || maxPriceCents != null

    /** Number of active refinements, shown as a badge on the filters button. */
    val activeCount: Int
        get() = listOf(sort != SortOption.RELEVANCE, onSaleOnly, topRatedOnly, priceRangeActive).count { it }
}

const val TOP_RATED_THRESHOLD = 4.0

internal fun List<UiResult>.applyFilters(filters: SearchFilters): List<UiResult> {
    val filtered = filter { item ->
        (!filters.onSaleOnly || item.discountPercent > 0) &&
            (!filters.topRatedOnly || item.rating >= TOP_RATED_THRESHOLD) &&
            (filters.minPriceCents == null || item.priceCents >= filters.minPriceCents) &&
            (filters.maxPriceCents == null || item.priceCents <= filters.maxPriceCents)
    }
    // sortedBy* are stable, so ties keep the server's relevance order.
    return when (filters.sort) {
        SortOption.RELEVANCE -> filtered
        SortOption.PRICE_ASC -> filtered.sortedBy { it.priceCents }
        SortOption.PRICE_DESC -> filtered.sortedByDescending { it.priceCents }
        SortOption.DISCOUNT -> filtered.sortedByDescending { it.discountPercent }
        SortOption.RATING -> filtered.sortedByDescending { it.rating }
    }
}
