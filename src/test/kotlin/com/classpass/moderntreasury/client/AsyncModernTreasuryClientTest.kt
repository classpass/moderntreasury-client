package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.classpass.moderntreasury.model.LedgerAccountBalance
import com.classpass.moderntreasury.model.LedgerAccountBalanceItem
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.BasicCredentials
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val ORG_ID = "org_id"
private const val API_KEY = "api_key"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncModernTreasuryClientTest {

    private val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var client: ModernTreasuryClient

    @BeforeAll
    fun beforeAll() {
        wireMockServer.start()
        val baseUrl = "http://localhost:${wireMockServer.port()}"
        val config = ModernTreasuryConfig(ORG_ID, API_KEY, baseUrl)
        client = asyncModernTreasuryClient(config)

        configureFor("localhost", wireMockServer.port())
    }

    @BeforeEach
    fun setup() {
        wireMockServer.resetAll()
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }

    @Test
    fun `verify basic auth is on the request`() {
        stubFor(get(urlEqualTo("/ping")).willReturn(ok("""{"foo": "bar"}""")))

        client.ping().get()

        val expectedCredentials = BasicCredentials(ORG_ID, API_KEY)
        verify(getRequestedFor(urlEqualTo("/ping")).withBasicAuth(expectedCredentials))
    }

    @Test
    fun testGetBalance() {
        val response = """
        {
          "pending": [
            {
              "credits": 6,
              "debits": 23,
              "amount": -17,
              "currency": "USD"
            }
          ],
          "posted": [
            {
              "credits": 0,
              "debits": 11,
              "amount": -11,
              "currency": "USD"
            }
          ]
        }    
        """.trimIndent()

        stubFor(get(urlMatching("/ledger_accounts/.+/balance")).willReturn(ok(response)))
        val expectedDeserialized = LedgerAccountBalance(
            pending = listOf(LedgerAccountBalanceItem(6, 23, -17, "USD")),
            posted = listOf(LedgerAccountBalanceItem(0, 11, -11, "USD"))
        )
        val actual = client.getLedgerAccountBalance("12345").get()
        assertThat(actual).isEqualTo(expectedDeserialized)
    }
}
