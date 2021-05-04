package com.classpass.moderntreasury.client

import com.classpass.clients.BadResponseException
import com.classpass.clients.deserialize2xx
import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.classpass.moderntreasury.exception.RateLimitTimeoutException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.util.concurrent.RateLimiter
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.Dsl
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.CompletableFuture

interface ModernTreasuryClient : Closeable {
    fun ping(): CompletableFuture<Void>
}

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

    override fun ping(): CompletableFuture<Void> {
        return get<Void, Void>("/ping")
    }

    private inline fun <T, reified R> get(
        endpoint: String,
    ) = executeRequest<R> {
        prepareGet("$baseUrl$endpoint")
    }

    internal inline fun <reified T> executeRequest(
        crossinline block: AsyncHttpClient.() -> BoundRequestBuilder
    ): CompletableFuture<T> =
        CompletableFuture.supplyAsync {
            if (!rateLimiter.tryAcquire(Duration.ofMillis(rateLimitTimeoutMs))) {
                throw RateLimitTimeoutException(rateLimitTimeoutMs)
            }
        }.thenCompose {
            val requestBuilder = block.invoke(httpClient)
            requestBuilder.execute()
                .deserialize2xx<T>(objectReader) {
                    throw BadResponseException(it)
                }
        }

    /**
     * Closes the underlying AsyncHttpClient, making this ModernTreasuryClient unusable.
     */
    override fun close() {
        httpClient.close()
    }
}
