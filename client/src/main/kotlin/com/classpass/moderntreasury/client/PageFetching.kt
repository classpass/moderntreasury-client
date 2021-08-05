package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.model.ModernTreasuryPage
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil

private const val PER_PAGE_MAX = 100

/**
 * Fetch all the pages of a paginated response from ModernTreasury.
 */
fun <T> ModernTreasuryClient.fetchAllPages(
    fetchPage: ModernTreasuryClient.(page: Int, perPage: Int) -> CompletableFuture<ModernTreasuryPage<T>>
): CompletableFuture<List<T>> {
    val pageOne = fetchPage(1, PER_PAGE_MAX).get()
    val totalPages = ceil(pageOne.totalCount / pageOne.perPage.toDouble()).toInt()
    val remainingPages =
        (1..totalPages).map { pageNumber ->
            fetchPage(pageNumber, PER_PAGE_MAX)
        }
    val pageFutures = listOf(CompletableFuture.completedFuture(pageOne)).plus(remainingPages)
    val allFetched = CompletableFuture.allOf(*pageFutures.toTypedArray())
    return allFetched.thenApply { _ ->
        pageFutures.flatMap { it.join().content }
    }
}
