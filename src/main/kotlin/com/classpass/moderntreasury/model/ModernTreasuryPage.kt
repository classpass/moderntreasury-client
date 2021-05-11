package com.classpass.moderntreasury.model

import org.asynchttpclient.Response

// ModernTreasury uses these headers to communicate pagination information on their responses
internal const val PAGE_HEADER = "x-page"
internal const val PER_PAGE_HEADER = "x-per-page"
internal const val TOTAL_COUNT_HEADER = "x-total-count"

/**
 * Represents a page of content returned from a paginated "List" endpoint on ModernTreasury.
 */
class ModernTreasuryPage<T>(
    pageInfo: ModernTreasuryPageInfo,
    /**
     * The content of the current page
     */
    val content: List<T>
) : ModernTreasuryPageInfo by pageInfo

/**
 * Metadata about a page of content from the ModernTreasury API. includes the current page, number of records per page,
 * and the total number of records.
 */
interface ModernTreasuryPageInfo {
    /**
     * The current page
     */
    val page: Int

    /**
     * The number of records on each page
     */
    val perPage: Int

    /**
     * The total number of records
     */
    val totalCount: Int
}

private data class ModernTreasuryPageInfoImp(
    override val page: Int,
    override val perPage: Int,
    override val totalCount: Int
) : ModernTreasuryPageInfo

/**
 * Extract pagination metadata from a ModernTreasury API response by examining its headers.
 */
fun Response.extractPageInfo(): ModernTreasuryPageInfo? = this.let { response ->
    val page = response.getHeader(PAGE_HEADER)?.toInt()
    val perPage = response.getHeader(PER_PAGE_HEADER)?.toInt()
    val totalCount = response.getHeader(TOTAL_COUNT_HEADER)?.toInt()
    return if (page == null || perPage == null || totalCount == null) {
        null
    } else {
        ModernTreasuryPageInfoImp(page, perPage, totalCount)
    }
}
