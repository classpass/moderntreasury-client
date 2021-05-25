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
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.junit.jupiter.api.Test
import java.util.UUID

class LedgerAccountTests : WireMockClientTest() {

    @Test
    fun testGetBalance() {
        stubFor(get(urlMatching("/ledger_accounts/.+/balance")).willReturn(ledgerAccountBalanceResponse))
        val expectedDeserialized = LedgerAccountBalance(
            pending = listOf(LedgerAccountBalanceItem(6, 23, -17, "USD")),
            posted = listOf(LedgerAccountBalanceItem(0, 11, -11, "USD"))
        )
        val actual = client.getLedgerAccountBalance(UUID.randomUUID()).get()
        assertThat(actual).isEqualTo(expectedDeserialized)
    }

    @Test
    fun `createLedgerAccount makes a well-formatted request body and deserializes responses properly`() {
        val uuid = UUID.randomUUID()
        val request = CreateLedgerAccountRequest(
            "the_name",
            "the_description",
            NormalBalanceType.CREDIT,
            uuid,
            "idempotencykey"
        )
        val expectedRequestJson = """
            {
                "name": "the_name",
                "description": "the_description",
                "normal_balance": "credit",
                "ledger_id": "$uuid",
                "metadata": {}
            }
        """
        stubFor(
            post(urlMatching("/ledger_accounts"))
                .withRequestBody(equalToJson(expectedRequestJson))
                .willReturn(ledgerAccountResponse(uuid))
        )

        val actualResponse = client.createLedgerAccount(request).get()
        val expectedDeserialized = LedgerAccount(
            id = actualResponse.id,
            name = "Operating Bank Account",
            ledgerId = uuid,
            description = null,
            normalBalance = NormalBalanceType.DEBIT,
            lockVersion = 23,
            metadata = emptyMap(),
            liveMode = true,
        )
        assertThat(actualResponse).isEqualTo(expectedDeserialized)
    }
}
