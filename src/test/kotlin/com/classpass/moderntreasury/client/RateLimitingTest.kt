package com.classpass.moderntreasury.client

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
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RateLimitingTest {
    private val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private val rateLimiter: RateLimiter = mock()
    private lateinit var client: AsyncModernTreasuryClient
    private lateinit var baseUrl: String

    @BeforeAll
    fun beforeAll() {
        wireMockServer.start()
        baseUrl = "http://localhost:${wireMockServer.port()}"
        val asyncHttpClient = Dsl.asyncHttpClient()
        client = AsyncModernTreasuryClient(asyncHttpClient, baseUrl, rateLimiter)

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
     * Check that we call rateLimiter.acquire() at some point during request execution.
     */
    @Test
    fun testRatesAreLimited() {
        stubFor(get(anyUrl()).willReturn(ok("""{"foo": "bar"}""")))
        client.executeRequest<Any?> { prepareGet("$baseUrl/foo") }.get()
        verify(rateLimiter).acquire()
    }
}
