/**
 * Copyright 2021 ClassPass
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

import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.classpass.moderntreasury.exception.MissingPaginationHeadersException
import com.classpass.moderntreasury.exception.RateLimitTimeoutException
import com.classpass.moderntreasury.exception.toModernTreasuryException
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountId
import com.classpass.moderntreasury.model.LedgerId
import com.classpass.moderntreasury.model.LedgerTransaction
import com.classpass.moderntreasury.model.LedgerTransactionId
import com.classpass.moderntreasury.model.ModernTreasuryPage
import com.classpass.moderntreasury.model.extractPageInfo
import com.classpass.moderntreasury.model.request.CreateLedgerAccountRequest
import com.classpass.moderntreasury.model.request.CreateLedgerRequest
import com.classpass.moderntreasury.model.request.CreateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.DateQuery
import com.classpass.moderntreasury.model.request.IdempotentRequest
import com.classpass.moderntreasury.model.request.InstantQuery
import com.classpass.moderntreasury.model.request.UpdateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.toQueryParams
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.google.common.util.concurrent.RateLimiter
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.Dsl
import org.asynchttpclient.Response
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

/**
 * Create a ModernTreasuryClient backed by an AsyncHttpClient. You should use a single instance of ModernTreasuryClient
 * for your application's entire lifecycle.
 */
fun asyncModernTreasuryClient(
    config: ModernTreasuryConfig,
    responseCallback: ResponseCallback = DO_NOTHING_RESPONSE_CALLBACK
): ModernTreasuryClient =
    AsyncModernTreasuryClient.create(config, responseCallback)

internal class AsyncModernTreasuryClient(
    internal val httpClient: AsyncHttpClient,
    private val baseUrl: String,
    private val rateLimiter: RateLimiter,
    private val rateLimitTimeoutMs: Long,
    private val responseCallback: ResponseCallback,
) : ModernTreasuryClient {
    companion object {
        fun create(config: ModernTreasuryConfig, responseCallback: ResponseCallback): AsyncModernTreasuryClient {
            val clientConfig = Dsl.config()
                .setRealm(Dsl.basicAuthRealm(config.organizationId, config.apiKey).setUsePreemptiveAuth(true))
                .setConnectTimeout(config.connectTimeoutMs)
                .setReadTimeout(config.readTimeoutMs)
                .setRequestTimeout(config.requestTimeoutMs)
                .setHttpClientCodecMaxHeaderSize(config.httpClientCodecMaxHeaderSize)
                .build()
            val asyncHttpClient = Dsl.asyncHttpClient(clientConfig)
            val rateLimiter = RateLimiter.create(config.rateLimit)
            return AsyncModernTreasuryClient(
                asyncHttpClient,
                config.baseUrl,
                rateLimiter,
                config.rateLimitTimeoutMs,
                responseCallback,
            )
        }
    }

    private val objectMapper: ObjectMapper = JsonMapper.builder()
        .addModules(KotlinModule(), JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
        .build()

    private val objectReader = objectMapper.reader()

    override fun createLedgerAccount(
        request: CreateLedgerAccountRequest
    ): CompletableFuture<LedgerAccount> = post("/ledger_accounts", request)

    override fun getLedgerAccount(ledgerAccountId: LedgerAccountId, balancesAsOfDate: LocalDate?): CompletableFuture<LedgerAccount> {
        val queryParams = balancesAsOfDate?.let {
            mapOf("balances[as_of_date]" to listOf(it.toString()))
        } ?: emptyMap()

        return get("/ledger_accounts/$ledgerAccountId", queryParams)
    }

    override fun getLedgerAccounts(
        ledgerAccountIds: List<LedgerAccountId>,
        balancesAsOfDate: LocalDate?,
        page: Int,
        perPage: Int
    ): CompletableFuture<ModernTreasuryPage<LedgerAccount>> {
        val queryParams = mapOf(
            "balances[as_of_date]" to listOfNotNull(balancesAsOfDate?.toString()),
            "id[]" to ledgerAccountIds.map { it.toString() },
            "page" to listOf(page.toString()),
            "per_page" to listOf(perPage.toString())
        )
        return getPaginated("/ledger_accounts", queryParams)
    }

    override fun getLedgerTransaction(id: LedgerTransactionId): CompletableFuture<LedgerTransaction> =
        get("/ledger_transactions/$id")

    override fun getLedgerTransactions(
        ledgerId: LedgerId?,
        ledgerAccountId: LedgerAccountId?,
        metadata: Map<String, String>,
        effectiveDate: DateQuery?,
        postedAt: InstantQuery?,
        updatedAt: InstantQuery?,
        page: Int,
        perPage: Int
    ): CompletableFuture<ModernTreasuryPage<LedgerTransaction>> {
        val queryParams = mapOf(
            "ledger_id" to listOfNotNull(ledgerId?.toString()),
            "ledger_account_id" to listOfNotNull(ledgerAccountId?.toString()),
            "page" to listOf(page.toString()),
            "per_page" to listOf(perPage.toString())
        ).plus(effectiveDate?.toQueryParams("effective_date") ?: emptyMap())
            .plus(postedAt?.toQueryParams("posted_at") ?: emptyMap())
            .plus(updatedAt?.toQueryParams("updated_at") ?: emptyMap())
            .plus(metadata.toQueryParams())

        return getPaginated("/ledger_transactions", queryParams)
    }

    override fun createLedgerTransaction(request: CreateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> =
        post("/ledger_transactions", request)

    override fun updateLedgerTransaction(request: UpdateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> =
        patch("/ledger_transactions/${request.id}", request)

    override fun createLedger(request: CreateLedgerRequest): CompletableFuture<Ledger> =
        post("/ledgers", request)

    override fun deleteLedger(id: LedgerId): CompletableFuture<Unit> =
        delete("/ledgers/$id")

    override fun ping(): CompletableFuture<Map<String, String>> {
        return get("/ping")
    }

    internal inline fun <reified R> get(
        endpoint: String,
        queryParams: Map<String, List<String>> = emptyMap()
    ) = executeRequest<R> {
        prepareGet("$baseUrl$endpoint").setQueryParams(queryParams)
    }

    internal inline fun <reified R> getPaginated(
        endpoint: String,
        queryParams: Map<String, List<String>> = emptyMap()
    ) = executePaginatedRequest<R> {
        prepareGet("$baseUrl$endpoint").setQueryParams(queryParams)
    }

    internal inline fun <reified R> post(
        endpoint: String,
        requestBody: IdempotentRequest,
    ) = executeRequest<R> {
        preparePost("$baseUrl$endpoint")
            .setBody(objectMapper.writeValueAsBytes(requestBody))
            .addHeader("Content-Type", "application/json")
            .addHeader("Idempotency-Key", requestBody.idempotencyKey)
    }

    private inline fun <T, reified R> patch(
        endpoint: String,
        requestBody: T
    ) = executeRequest<R> {
        preparePatch("$baseUrl$endpoint")
            .setBody(objectMapper.writeValueAsBytes(requestBody))
            .addHeader("Content-Type", "application/json")
    }

    private inline fun <reified R> delete(
        endpoint: String
    ) = executeRequest<R> {
        prepareDelete("$baseUrl$endpoint")
            .addHeader("Content-Type", "application/json")
    }

    internal inline fun <reified T> executeRequest(
        crossinline block: AsyncHttpClient.() -> BoundRequestBuilder
    ) = sendRequest(block).thenApply<T>(this::deserializeResponse)

    internal inline fun <reified T> executePaginatedRequest(
        crossinline block: AsyncHttpClient.() -> BoundRequestBuilder
    ) = sendRequest(block).thenApply<ModernTreasuryPage<T>>(this::deserializePaginatedResponse)

    internal inline fun sendRequest(
        crossinline block: AsyncHttpClient.() -> BoundRequestBuilder
    ): CompletableFuture<Response> {
        return CompletableFuture.supplyAsync {
            if (!rateLimiter.tryAcquire(Duration.ofMillis(rateLimitTimeoutMs))) {
                throw RateLimitTimeoutException(rateLimitTimeoutMs)
            }
            val response = block.invoke(httpClient).execute().get()
            val rateLimitRemaining: String? = response.getHeader("X-Rate-Limit-Remaining")
            rateLimitRemaining?.let { responseCallback.rateLimitRemaining(it.toInt()) }
            if (response.statusCode in 200..299) {
                response
            } else {
                throw response.toModernTreasuryException(objectReader)
            }
        }
    }

    internal inline fun <reified T> deserializeResponse(response: Response): T =
        objectReader.forType(jacksonTypeRef<T>()).readValue(response.responseBody)

    internal inline fun <reified T> deserializePaginatedResponse(response: Response): ModernTreasuryPage<T> {
        val tr = jacksonTypeRef<T>() // remove this line to break deserialization for mysterious reflection-related reasons
        val content = deserializeResponse<List<T>>(response)
        val pageInfo = response.extractPageInfo() ?: throw MissingPaginationHeadersException(response)
        return ModernTreasuryPage(pageInfo, content)
    }

    /**
     * Closes the underlying AsyncHttpClient, making this ModernTreasuryClient unusable.
     */
    override fun close() {
        httpClient.close()
    }
}
