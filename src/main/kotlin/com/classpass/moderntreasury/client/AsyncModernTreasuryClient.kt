package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.classpass.moderntreasury.exception.MissingPaginationHeadersException
import com.classpass.moderntreasury.exception.RateLimitTimeoutException
import com.classpass.moderntreasury.exception.toModernTreasuryException
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountBalance
import com.classpass.moderntreasury.model.LedgerTransaction
import com.classpass.moderntreasury.model.ModernTreasuryPage
import com.classpass.moderntreasury.model.extractPageInfo
import com.classpass.moderntreasury.model.request.CreateLedgerAccountRequest
import com.classpass.moderntreasury.model.request.CreateLedgerRequest
import com.classpass.moderntreasury.model.request.CreateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.IdempotentRequest
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
 * for your application's entire lifecycle
 */
fun asyncModernTreasuryClient(config: ModernTreasuryConfig): ModernTreasuryClient = AsyncModernTreasuryClient.create(config)

internal class AsyncModernTreasuryClient(
    internal val httpClient: AsyncHttpClient,
    private val baseUrl: String,
    private val rateLimiter: RateLimiter,
    private val rateLimitTimeoutMs: Long
) : ModernTreasuryClient {
    companion object {
        fun create(config: ModernTreasuryConfig): AsyncModernTreasuryClient {
            val clientConfig = Dsl.config()
                .setRealm(Dsl.basicAuthRealm(config.organizationId, config.apiKey).setUsePreemptiveAuth(true))
                .setConnectTimeout(config.connectTimeoutMs)
                .setReadTimeout(config.readTimeoutMs)
                .setRequestTimeout(config.requestTimeoutMs)
                .build()
            val asyncHttpClient = Dsl.asyncHttpClient(clientConfig)
            val rateLimiter = RateLimiter.create(config.rateLimit)
            return AsyncModernTreasuryClient(asyncHttpClient, config.baseUrl, rateLimiter, config.rateLimitTimeoutMs)
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

    override fun getLedgerAccount(ledgerAccountId: String): CompletableFuture<LedgerAccount> =
        get("/ledger_accounts/$ledgerAccountId")

    override fun getLedgerAccountBalance(
        ledgerAccountId: String,
        asOfDate: LocalDate?
    ): CompletableFuture<LedgerAccountBalance> {
        val queryParams = asOfDate?.let {
            mapOf("as_of_date" to listOf(it.toString()))
        } ?: emptyMap()

        return get("/ledger_accounts/$ledgerAccountId/balance", queryParams)
    }

    override fun getLedgerTransaction(id: String): CompletableFuture<LedgerTransaction> =
        get("/ledger_transactions/$id")

    override fun getLedgerTransactions(
        ledgerId: String?,
        metadata: Map<String, String>
    ): CompletableFuture<ModernTreasuryPage<LedgerTransaction>> {
        val queryParams = mapOf(
            "ledger_id" to listOfNotNull(ledgerId),
        ).plus(metadata.toQueryParams())

        return getPaginated("/ledger_transactions", queryParams)
    }

    override fun createLedgerTransaction(request: CreateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> =
        post("/ledger_transactions", request)

    override fun updateLedgerTransaction(request: UpdateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> =
        patch("/ledger_transactions/${request.id}", request)

    override fun createLedger(request: CreateLedgerRequest): CompletableFuture<Ledger> =
        post("/ledgers", request)

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