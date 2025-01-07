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
package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.exception.RateLimitTimeoutException
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.google.common.util.concurrent.RateLimiter
import org.asynchttpclient.Dsl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import java.time.Duration
import java.util.concurrent.CompletionException
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RateLimitingTest {
    private val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private val rateLimiter: RateLimiter = spy(RateLimiter.create(1.0))
    private val rateLimiterTimeoutMs = 10L
    private lateinit var client: AsyncModernTreasuryClient
    private lateinit var baseUrl: String

    @BeforeAll
    fun beforeAll() {
        wireMockServer.start()
        baseUrl = "http://localhost:${wireMockServer.port()}"
        val asyncHttpClient = Dsl.asyncHttpClient()
        client = AsyncModernTreasuryClient(
            asyncHttpClient,
            baseUrl,
            rateLimiter,
            rateLimiterTimeoutMs,
            DO_NOTHING_RESPONSE_CALLBACK,
        )

        configureFor("localhost", wireMockServer.port())
    }

    @BeforeEach
    fun setup() {
        reset(rateLimiter)
        wireMockServer.resetAll()
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }

    /**
     * Check that we call rateLimiter.tryAcquire() at some point during request execution.
     */
    @Test
    fun testRatesAreLimited() {
        stubFor(get(anyUrl()).willReturn(ok("""{"foo": "bar"}""")))
        client.executeRequest<Any?> { prepareGet("$baseUrl/foo") }.get()
        verify(rateLimiter).tryAcquire(any<Duration>())
    }

    /**
     * Try requesting three permits when the rate limiter is configured to allow 1 qps. With a timeout of 10ms, the last
     * request should fail with a RateLimitTimeoutException
     */
    @Test
    fun testRateLimiterTimeout() {
        stubFor(get(anyUrl()).willReturn(ok("""{"foo": "bar"}""")))
        val completionException = assertFailsWith(CompletionException::class) {
            List(3) {
                client.executeRequest<Any?> {
                    prepareGet("$baseUrl/foo")
                }
            }.map { it.join() }
        }
        assertTrue(completionException.cause is RateLimitTimeoutException)
    }
}
