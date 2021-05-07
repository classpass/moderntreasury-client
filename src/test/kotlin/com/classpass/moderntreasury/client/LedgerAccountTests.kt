package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountBalance
import com.classpass.moderntreasury.model.LedgerAccountBalanceItem
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.request.CreateLedgerAccountRequest
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.junit.jupiter.api.Test

class LedgerAccountTests : WireMockClientTest() {

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

    @Test
    fun `createLedgerAccount makes a well-formatted request body and deserializes responses properly`() {
        val response = """
            {
                "id": "f1c7-xxxxx",
                "object": "ledger_account",
                "name": "Operating Bank Account",
                "ledger_id": "89c8-xxxxx",
                "description": null,
                "normal_balance": "debit",
                "metadata": {},
                "live_mode": true,
                "created_at": "2020-08-04T16:54:32Z",
                "updated_at": "2020-08-04T16:54:32Z"
            }
        """

        val request = CreateLedgerAccountRequest("the_name", "the_description", NormalBalanceType.CREDIT, "1234")
        val expectedRequestJson = """
            {
                "name": "the_name",
                "description": "the_description",
                "normal_balance": "credit",
                "ledger_id": "1234",
                "metadata": {}
            }
        """
        stubFor(
            post(urlMatching("/ledger_accounts"))
                .withRequestBody(equalToJson(expectedRequestJson))
                .willReturn(ok(response))
        )

        val actualResponse = client.createLedgerAccount(request).get()
        val expectedDeserialized = LedgerAccount(
            id = "f1c7-xxxxx",
            name = "Operating Bank Account",
            ledgerId = "89c8-xxxxx",
            description = null,
            normalBalance = NormalBalanceType.DEBIT,
            metadata = emptyMap(),
            liveMode = true,
        )
        assertThat(actualResponse).isEqualTo(expectedDeserialized)
    }
}
