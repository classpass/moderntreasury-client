package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.model.ModernTreasuryPage
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil

/**
 * This is the largest page size the ModernTreasury API will allow
 */
private const val PER_PAGE_MAX = 100

/**
 * Asynchronously fetch all the pages of a paginated ModernTreasury endpoint. Returns a CompletableFuture of a list
 * comprising the content of all the pages concatenated
 *
 * Example usage:
 * mtClient.fetchAllPages { page, perPage -> getLedgerTransactions(ledgerId, null, page = page, perPage = perPage) }
 */
fun <T> ModernTreasuryClient.fetchAllPages(
    fetchPage: ModernTreasuryClient.(paginatedFnPage: Int, paginatedFnPerPage: Int) -> CompletableFuture<ModernTreasuryPage<T>>,
    perPage: Int = PER_PAGE_MAX
): CompletableFuture<List<T>> =
    fetchPage(1, perPage).thenApply { pageOne ->
        val totalPages = ceil(pageOne.totalCount / pageOne.perPage.toDouble()).toInt()
        val remainingPages = (2..totalPages).map { pageNumber ->
            fetchPage(pageNumber, perPage)
        }
        val pageFutures = listOf(CompletableFuture.completedFuture(pageOne)).plus(remainingPages)
        pageFutures.flatMap { it.join().content }
    }
