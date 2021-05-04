package com.classpass.moderntreasury.client

import com.classpass.clients.BadResponseException
import com.classpass.clients.deserialize2xx
import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.classpass.moderntreasury.exception.RateLimitTimeoutException
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccountBalance
import com.classpass.moderntreasury.model.NormalBalanceType
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.google.common.util.concurrent.RateLimiter
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.Dsl
import java.io.Closeable
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

interface ModernTreasuryClient : Closeable {
    fun createLedgerAccount(
        name: String,
        description: String?,
        normalBalanceType: NormalBalanceType,
        ledgerId: String,
        metadata: Map<String, String> = emptyMap()
    ): Ledger

    fun getLedgerAccountBalance(
        ledgerAccountId: String,
        /**
         * The date of the balance in local time. Defaults to today's date.
         */
        asOfDate: LocalDate? = null
    ): CompletableFuture<LedgerAccountBalance>

    fun ping(): CompletableFuture<Map<String, String>>
}

/**
 * Create a ModernTreasuryClient backed by an AsyncHttpClient. You should use a single instance of ModernTreasuryClient
 * for your application's entire lifecycle
 */
fun asyncModernTreasuryClient(config: ModernTreasuryConfig): ModernTreasuryClient = AsyncModernTreasuryClient.create(config)

internal class AsyncModernTreasuryClient(
    private val httpClient: AsyncHttpClient,
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
        .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
        .build()

    private val objectReader = objectMapper.reader()

    override fun createLedgerAccount(
        name: String,
        description: String?,
        normalBalanceType: NormalBalanceType,
        ledgerId: String,
        metadata: Map<String, String>
    ): Ledger {
        TODO("Not yet implemented")
    }

    override fun getLedgerAccountBalance(
        ledgerAccountId: String,
        asOfDate: LocalDate?
    ): CompletableFuture<LedgerAccountBalance> {
        val queryParams = asOfDate?.let {
            mapOf("as_of_date" to listOf(it.toString()))
        } ?: emptyMap()

        return get("/ledger_accounts/$ledgerAccountId/balance", queryParams)
    }

    override fun ping(): CompletableFuture<Map<String, String>> {
        return get("/ping")
    }

    private inline fun <reified R> get(
        endpoint: String,
        queryParams: Map<String, List<String>> = emptyMap()
    ) = executeRequest<R> {
        prepareGet("$baseUrl$endpoint").setQueryParams(queryParams)
    }

    internal inline fun <reified T> executeRequest(
        crossinline block: AsyncHttpClient.() -> BoundRequestBuilder
    ): CompletableFuture<T> {
        val typeRef = jacksonTypeRef<T>()
        return CompletableFuture.supplyAsync {
            if (!rateLimiter.tryAcquire(Duration.ofMillis(rateLimitTimeoutMs))) {
                throw RateLimitTimeoutException(rateLimitTimeoutMs)
            }
        }.thenCompose {
            val requestBuilder = block.invoke(httpClient)
            requestBuilder.execute()
                .deserialize2xx(typeRef, objectReader) {
                    throw BadResponseException(it)
                }
        }
    }

    /**
     * Closes the underlying AsyncHttpClient, making this ModernTreasuryClient unusable.
     */
    override fun close() {
        httpClient.close()
    }
}
