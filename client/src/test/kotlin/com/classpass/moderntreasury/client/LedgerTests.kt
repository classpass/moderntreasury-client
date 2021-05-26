package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerId
import com.classpass.moderntreasury.model.request.CreateLedgerRequest
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.junit.jupiter.api.Test
import java.util.UUID

class LedgerTests : WireMockClientTest() {
    @Test
    fun `createLedger request and response`() {
        val uuid = UUID.randomUUID()
        val request = CreateLedgerRequest(
            "ledger_name",
            "description",
            "eur",
            "asdf234"
        )

        val expectedRequestJson = """
        {
           "name" : "ledger_name",
           "description" : "description",
           "currency" : "eur",
           "metadata" : { }
         }
        """
        stubFor(
            post(urlMatching("/ledgers")).withRequestBody(equalToJson(expectedRequestJson))
                .willReturn(ledgerResponse(uuid))
        )

        val expectedLedger = Ledger(
            LedgerId(uuid),
            "Business Ledger",
            null,
            "USD",
            emptyMap(),
            true
        )

        val actualLedger = client.createLedger(request).get()

        assertThat(actualLedger).isEqualTo(expectedLedger)
    }
}
