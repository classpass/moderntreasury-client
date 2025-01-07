/**
 * Copyright 2025 ClassPass
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
package com.classpass.moderntreasury.model

import org.asynchttpclient.Response

// ModernTreasury uses these headers to communicate pagination information on their responses
internal const val AFTER_CURSOR = "x-after-cursor"
internal const val PER_PAGE_HEADER = "x-per-page"

/**
 * Represents a page of content returned from a paginated "List" endpoint on ModernTreasury.
 */
data class ModernTreasuryPage<T>(
    private val pageInfo: ModernTreasuryPageInfo,
    /**
     * The content of the current page
     */
    val content: List<T>
) : ModernTreasuryPageInfo by pageInfo

/**
 * Metadata about a page of content from the ModernTreasury API.
 */
interface ModernTreasuryPageInfo {
    /**
     * The cursor for the next page
     */
    val afterCursor: String?

    /**
     * The number of records on each page
     */
    val perPage: Int
}

private data class ModernTreasuryPageInfoImp(
    override val afterCursor: String?,
    override val perPage: Int,
) : ModernTreasuryPageInfo

/**
 * Extract pagination metadata from a ModernTreasury API response by examining its headers.
 */
fun Response.extractPageInfo(): ModernTreasuryPageInfo? = this.let { response ->
    val afterCursor = response.getHeader(AFTER_CURSOR)
    val perPage = response.getHeader(PER_PAGE_HEADER)?.toInt()
    return if (perPage == null) {
        null
    } else {
        ModernTreasuryPageInfoImp(afterCursor?.ifEmpty { null }, perPage)
    }
}
