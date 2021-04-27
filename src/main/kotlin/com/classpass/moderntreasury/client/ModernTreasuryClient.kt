package com.classpass.moderntreasury.client

import com.classpass.clients.BadResponseException
import com.classpass.clients.deserialize2xx
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.BoundRequestBuilder
import java.util.concurrent.CompletableFuture

interface ModernTreasuryClient {
    fun ping(): CompletableFuture<Void>
}

internal class AsyncModernTreasuryClient(
    private val baseUrl: String,
    private val httpClient: AsyncHttpClient,
) : ModernTreasuryClient {
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

    private inline fun <reified T> executeRequest(
        block: AsyncHttpClient.() -> BoundRequestBuilder
    ): CompletableFuture<T> {
        val requestBuilder = block.invoke(httpClient)

        return requestBuilder.execute()
            .deserialize2xx(objectReader) {
                throw BadResponseException(it)
            }
    }
}
