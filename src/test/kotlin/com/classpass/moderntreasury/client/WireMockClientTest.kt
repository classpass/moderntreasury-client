package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class WireMockClientTest {

    internal val ORG_ID = "org_id"
    internal val API_KEY = "api_key"

    private val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    lateinit var client: ModernTreasuryClient

    @BeforeAll
    fun beforeAll() {
        wireMockServer.start()
        client = buildClient()
        configureFor("localhost", wireMockServer.port())
    }

    internal open fun buildClient(): AsyncModernTreasuryClient {
        val baseUrl = "http://localhost:${wireMockServer.port()}"
        val config = ModernTreasuryConfig(ORG_ID, API_KEY, baseUrl)
        return AsyncModernTreasuryClient.create(config)
    }

    @BeforeEach
    fun setup() {
        wireMockServer.resetAll()
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }
}
