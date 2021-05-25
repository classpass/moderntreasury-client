package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.ok
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
    internal lateinit var client: AsyncModernTreasuryClient

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

    protected val ledgerAccountResponse: ResponseDefinitionBuilder = ok(
        """
            {
                "id": "f1c7-xxxxx",
                "object": "ledger_account",
                "name": "Operating Bank Account",
                "ledger_id": "89c8-xxxxx",
                "description": null,
                "normal_balance": "debit",
                "lock_version": "23",
                "metadata": {},
                "live_mode": true,
                "created_at": "2020-08-04T16:54:32Z",
                "updated_at": "2020-08-04T16:54:32Z"
            }
        """
    )

    protected val ledgerAccountBalanceResponse = ok(
        """
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
        """
    )

    protected val ledgerTransactionResponse = ok(
        """
        {
          "id": "4f5b1dd9-xxx123",
          "object": "ledger_transaction",
          "live_mode": false,
          "external_id": "zwt3-xxx123",
          "ledgerable_type": null,
          "ledgerable_id": null,
          "ledger_id": "0aa9c435-xxx123",
          "description": "test 3 pending",
          "status": "pending",
          "ledger_entries": [
            {
              "id": "4492f794-xxx123",
              "object": "ledger_entry",
              "live_mode": false,
              "amount": 6,
              "direction": "credit",
              "ledger_account_id": "f3e54ff6-xxx123",
              "ledger_transaction_id": "4f5b1dd9-xxx123",
              "discarded_at": null,
              "created_at": "2021-05-04T21:44:08Z",
              "updated_at": "2021-05-04T21:44:08Z"
            }
          ],
          "posted_at": "2020-10-20T19:11:07Z",
          "effective_date": "2021-05-04",
          "metadata": {},
          "created_at": "2021-05-04T21:44:08Z",
          "updated_at": "2021-05-04T21:44:08Z"
        }
        """
    )

    protected val ledgerTransactionsListResponse = ok(
        ledgerTransactionResponse.build().body.let { responseElement ->
            "[$responseElement, $responseElement, $responseElement]"
        }
    )

    protected val ledgerResponse = ok(
        """
       {
            "id": "89c8bd30-e06a-4a79-b396-e6c7e13e7a12",
            "object": "ledger",
            "name": "Business Ledger",
            "description": null,
            "currency": "USD",
            "metadata": {},
            "live_mode": true,
            "created_at": "2020-08-04T16:48:05Z",
            "updated_at": "2020-08-04T16:48:05Z"
        } 
        """.trimIndent()
    )
}
