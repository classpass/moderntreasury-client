package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.classpass.moderntreasury.exception.MissingPaginationHeadersException
import com.classpass.moderntreasury.exception.RateLimitTimeoutException
import com.classpass.moderntreasury.exception.toModernTreasuryException
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountBalance
import com.classpass.moderntreasury.model.LedgerTransaction
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.ModernTreasuryPage
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.extractPageInfo
import com.classpass.moderntreasury.model.request.CreateLedgerAccountRequest
import com.classpass.moderntreasury.model.request.CreateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.IdempotentRequest
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import com.classpass.moderntreasury.model.request.RequestMetadata
import com.classpass.moderntreasury.model.request.UpdateLedgerTransactionRequest
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
import java.io.Closeable
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

interface ModernTreasuryClient : Closeable {

    fun createLedgerAccount(request: CreateLedgerAccountRequest): CompletableFuture<LedgerAccount>

    fun createLedgerAccount(
        name: String,
        description: String?,
        normalBalanceType: NormalBalanceType,
        ledgerId: String,
        idempotencyKey: String,
        metadata: RequestMetadata = emptyMap()
    ) = createLedgerAccount(
        CreateLedgerAccountRequest(
            name,
            description,
            normalBalanceType,
            ledgerId,
            idempotencyKey,
            metadata
        )
    )

    fun getLedgerAccountBalance(
        ledgerAccountId: String,
        /**
         * The date of the balance in local time. Defaults to today's date.
         */
        asOfDate: LocalDate? = null
    ): CompletableFuture<LedgerAccountBalance>

    fun getLedgerTransaction(
        /**
         * The ID of the ledger transaction
         */
        id: String
    ): CompletableFuture<LedgerTransaction>

    fun getLedgerTransactions(
        ledgerId: String? = null,
        /**
         * Key/Value metadata pairs to search transactions for. TODO are they ANDed together?
         */
        metadata: Map<String, String> = emptyMap()
    ): CompletableFuture<ModernTreasuryPage<LedgerTransaction>>

    fun createLedgerTransaction(
        effectiveDate: LocalDate,
        ledgerEntries: List<RequestLedgerEntry>,
        externalId: String,
        description: String?,
        status: LedgerTransactionStatus?,
        idempotencyKey: String,
        metadata: RequestMetadata = emptyMap()
    ): CompletableFuture<LedgerTransaction> = createLedgerTransaction(
        CreateLedgerTransactionRequest(
            effectiveDate,
            ledgerEntries,
            externalId,
            description,
            status,
            idempotencyKey,
            metadata
        )
    )

    fun createLedgerTransaction(
        request: CreateLedgerTransactionRequest
    ): CompletableFuture<LedgerTransaction>

    /**
     * Update a ledger transaction. Only pending ledger transactions can be updated. Returns the updated
     * LedgerTransaction.
     */
    fun updateLedgerTransaction(
        /**
         * The ID of the ledger transaction to update
         */
        id: String,
        description: String? = null,
        /**
         * To post the ledger transaction, use POSTED. To archive a pending ledger transaction, use ARCHIVED.
         */
        status: LedgerTransactionStatus? = null,
        /**
         * Note that updating entries will overwrite any prior entries on the ledger transaction.
         */
        ledgerEntries: List<RequestLedgerEntry>? = null,
        metadata: RequestMetadata = emptyMap()
    ): CompletableFuture<LedgerTransaction> =
        updateLedgerTransaction(UpdateLedgerTransactionRequest(id, description, status, ledgerEntries, metadata))

    /**
     * Update a ledger transaction. Only pending ledger transactions can be updated. Returns the updated
     * LedgerTransaction.
     */
    fun updateLedgerTransaction(request: UpdateLedgerTransactionRequest): CompletableFuture<LedgerTransaction>

    /**
     * Set a pending ledger transaction's status to ARCHIVED, effectively a soft delete.
     */
    fun archiveLedgerTransaction(
        /**
         * The ID of the ledger transaction to archive
         */
        id: String,
    ): CompletableFuture<LedgerTransaction> = updateLedgerTransaction(
        id,
        null,
        LedgerTransactionStatus.ARCHIVED
    )

    fun ping(): CompletableFuture<Map<String, String>>
}

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
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
        .build()

    private val objectReader = objectMapper.reader()

    override fun createLedgerAccount(
        request: CreateLedgerAccountRequest
    ): CompletableFuture<LedgerAccount> = post("/ledger_accounts", request)

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
    ): CompletableFuture<ModernTreasuryPage<LedgerTransaction>> =
        getPaginated<LedgerTransaction>("/ledger_transactions")

    override fun createLedgerTransaction(request: CreateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> =
        post("/ledger_transactions", request)

    override fun updateLedgerTransaction(request: UpdateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> =
        patch("/ledger_transactions/${request.id}", request)

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
    ) = sendRequest(block).thenApply<ModernTreasuryPage<T>>(this::deserializePagedResponse)

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

    internal inline fun <reified T> deserializePagedResponse(response: Response): ModernTreasuryPage<T> {
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
