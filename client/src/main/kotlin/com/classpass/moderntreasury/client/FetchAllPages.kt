/**
 * Copyright 2022 ClassPass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.model.ModernTreasuryPage
import java.util.concurrent.CompletableFuture

/**
 * This is the largest page size the ModernTreasury API will allow
 */
private const val PER_PAGE_MAX = 100

/**
 * Asynchronously fetch all the pages of a paginated ModernTreasury endpoint. Returns a CompletableFuture of a list
 * comprising the content of all the pages concatenated
 *
 * Example usage:
 * mtClient.fetchAllPages { afterCursor, perPage -> getLedgerTransactions(ledgerId, null, afterCursor = afterCursor?.let { LedgerTransactionId(it) }, perPage = perPage) }
 */
fun <T> ModernTreasuryClient.fetchAllPages(
    fetchPage: ModernTreasuryClient.(paginatedFnAfterCursor: String?, paginatedFnPerPage: Int) -> CompletableFuture<ModernTreasuryPage<T>>,
    perPage: Int = PER_PAGE_MAX
): CompletableFuture<List<T>> {

    val results: MutableList<T> = mutableListOf()
    var afterCursor: String? = null
    do {
        val aPage = fetchPage(afterCursor, perPage).get()
        results.addAll(aPage.content)
        afterCursor = aPage.afterCursor
    } while (afterCursor != null)

    return CompletableFuture.completedFuture(results)
}
