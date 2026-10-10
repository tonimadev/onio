package com.ipirangatech.onio.feature.search.impl

import com.ipirangatech.onio.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchFiltersTest {
    private fun item(id: Long, cents: Long, discount: Int = 0, rating: Double = 3.0) =
        UiResult(id, "p$id", "", Money.usd(cents), null, discount, rating)

    private val items = listOf(
        item(1, 5_000, discount = 0, rating = 4.5),
        item(2, 1_000, discount = 20, rating = 3.9),
        item(3, 9_000, discount = 10, rating = 4.0),
        item(4, 1_000, discount = 5, rating = 2.0),
    )

    private fun ids(filters: SearchFilters) = items.applyFilters(filters).map { it.id }

    @Test fun relevanceKeepsServerOrderAndNothingIsFilteredByDefault() {
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(SearchFilters()))
        assertEquals(0, SearchFilters().activeCount)
    }

    @Test fun sortsByPriceBothWaysKeepingTiesStable() {
        assertEquals(listOf(2L, 4L, 1L, 3L), ids(SearchFilters(sort = SortOption.PRICE_ASC)))
        assertEquals(listOf(3L, 1L, 2L, 4L), ids(SearchFilters(sort = SortOption.PRICE_DESC)))
    }

    @Test fun sortsByDiscountAndRating() {
        assertEquals(listOf(2L, 3L, 4L, 1L), ids(SearchFilters(sort = SortOption.DISCOUNT)))
        assertEquals(listOf(1L, 3L, 2L, 4L), ids(SearchFilters(sort = SortOption.RATING)))
    }

    @Test fun onSaleAndTopRatedFiltersCombine() {
        assertEquals(listOf(2L, 3L, 4L), ids(SearchFilters(onSaleOnly = true)))
        assertEquals(listOf(1L, 3L), ids(SearchFilters(topRatedOnly = true)))
        assertEquals(listOf(3L), ids(SearchFilters(onSaleOnly = true, topRatedOnly = true)))
    }

    @Test fun priceRangeIsInclusiveOnBothEnds() {
        assertEquals(listOf(1L, 2L, 4L), ids(SearchFilters(minPriceCents = 1_000, maxPriceCents = 5_000)))
        assertEquals(listOf(3L), ids(SearchFilters(minPriceCents = 9_000)))
    }

    @Test fun activeCountCountsEachRefinementOnce() {
        val filters = SearchFilters(SortOption.RATING, onSaleOnly = true, minPriceCents = 1, maxPriceCents = 2)
        assertEquals(3, filters.activeCount)
        assertEquals(4, filters.copy(topRatedOnly = true).activeCount)
    }
}
