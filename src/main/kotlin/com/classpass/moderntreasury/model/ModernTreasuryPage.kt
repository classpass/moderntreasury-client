package com.classpass.moderntreasury.model

// ModernTreasury uses these headers to communicate pagination information on their responses
internal const val PAGE_HEADER = "x-page"
internal const val PER_PAGE_HEADER = "x-per-page"
internal const val TOTAL_COUNT_HEADER = "x-total-count"

/**
 * Represents a page of content returned from a paginated "List" endpoint on ModernTreasury
 */
data class ModernTreasuryPage<T>(
    /**
     * The current page
     */
    val page: Int,
    /**
     * The number of records on each page
     */
    val perPage: Int,
    /**
     * The total number of records
     */
    val totalCount: Int,
    /**
     * The content of the current page
     */
    val content: List<T>
)
