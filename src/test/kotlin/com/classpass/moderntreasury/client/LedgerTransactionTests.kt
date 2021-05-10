package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.classpass.moderntreasury.model.LedgerEntry
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerTransaction
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.request.CreateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import com.classpass.moderntreasury.model.request.UpdateLedgerTransactionRequest
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class LedgerTransactionTests : WireMockClientTest() {

    @Test
    fun `LedgerTransaction response serialization`() {
        val expectedLedgerTransaction = LedgerTransaction(
            id = "4f5b1dd9-xxx123",
            description = "test 3 pending",
            status = LedgerTransactionStatus.PENDING,
            metadata = emptyMap(),
            ledgerEntries = listOf(
                LedgerEntry(
                    id = "4492f794-xxx123",
                    liveMode = false,
                    amount = 6,
                    direction = LedgerEntryDirection.CREDIT,
                    ledgerAccountId = "f3e54ff6-xxx123",
                ),
            ),
            postedAt = ZonedDateTime.of(2020, 10, 20, 19, 11, 7, 0, ZoneId.of("UTC")),
            effectiveDate = LocalDate.of(2021, 5, 4),
            ledgerId = "0aa9c435-xxx123",
            ledgerableType = null,
            ledgerableId = null,
            externalId = "zwt3-xxx123",
            liveMode = false
        )

        stubFor(get(urlMatching("/ledger_transactions/.+")).willReturn(ledgerTransactionResponse))
        val actualLedgerTransaction = client.getLedgerTransaction("asdf").get()
        assertThat(actualLedgerTransaction).isEqualTo(expectedLedgerTransaction)
    }

    @Test
    fun `createLedgerTransaction request serialization`() {
        val request = CreateLedgerTransactionRequest(
            LocalDate.of(2021, 5, 13),
            listOf(
                RequestLedgerEntry(
                    amount = 6,
                    direction = LedgerEntryDirection.DEBIT,
                    ledgerAccountId = "f3e54ff6-xxx123",
                ),
            ),
            "external-id",
            "description",
            LedgerTransactionStatus.PENDING,
            "idempotencykey",
            metadata = mapOf("present" to "here i am", "nullvalue" to null, "emptystring" to "")
        )

        val expectedRequestJson = """
            {
              "ledger_entries": [
                {
                  "amount": 6,
                  "direction": "debit",
                  "ledger_account_id": "f3e54ff6-xxx123"
                }
              ],
              "description": "description",
              "status": "pending",
              "metadata": {
                "present": "here i am",
                "nullvalue": null,
                "emptystring": ""
              },
              "effective_date": "2021-05-13",
              "external_id": "external-id"
            }
        """
        stubFor(
            post(urlMatching("/ledger_transactions$")).withRequestBody(equalToJson(expectedRequestJson))
                .willReturn(ledgerTransactionResponse)
        )

        assertDoesNotThrow { client.createLedgerTransaction(request).get() }
    }

    @Test
    fun `updateLedgerTransaction makes a well-formed request`() {
        val request = UpdateLedgerTransactionRequest(
            "the-id",
            "the-description",
            LedgerTransactionStatus.POSTED,
        )
        val expectedRequestJson = """
            {
               "description": "the-description",
               "status": "posted"
            }
        """
        stubFor(
            patch(urlMatching("/ledger_transactions/the-id$")).withRequestBody(equalToJson(expectedRequestJson))
                .willReturn(ledgerTransactionResponse)
        )
        assertDoesNotThrow { client.updateLedgerTransaction(request).get() }
    }

    @Test
    fun `getLedgerTransaction builds the url path properly`() {
        val id = "123abc"
        stubFor(get("/ledger_transactions/123abc").willReturn(ledgerTransactionResponse))
        assertDoesNotThrow { client.getLedgerTransaction(id).get() }
    }
}
