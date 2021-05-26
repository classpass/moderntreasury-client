package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.classpass.moderntreasury.model.LedgerAccountId
import com.classpass.moderntreasury.model.LedgerEntry
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerEntryId
import com.classpass.moderntreasury.model.LedgerId
import com.classpass.moderntreasury.model.LedgerTransaction
import com.classpass.moderntreasury.model.LedgerTransactionId
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.request.CreateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import com.classpass.moderntreasury.model.request.UpdateLedgerTransactionRequest
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
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
import java.util.UUID

class LedgerTransactionTests : WireMockClientTest() {

    @Test
    fun `LedgerTransaction response deserialization`() {
        val id = LedgerTransactionId(UUID.randomUUID())
        val ledgerId = LedgerId(UUID.randomUUID())
        val ledgerEntryId = LedgerEntryId(UUID.randomUUID())
        val ledgerAccountId = LedgerAccountId(UUID.randomUUID())
        val expectedLedgerTransaction = LedgerTransaction(
            id = id,
            description = "test 3 pending",
            status = LedgerTransactionStatus.PENDING,
            metadata = emptyMap(),
            ledgerEntries = listOf(
                LedgerEntry(
                    id = ledgerEntryId,
                    liveMode = false,
                    amount = 6,
                    direction = LedgerEntryDirection.CREDIT,
                    ledgerAccountId = ledgerAccountId,
                    lockVersion = null
                ),
            ),
            postedAt = ZonedDateTime.of(2020, 10, 20, 19, 11, 7, 0, ZoneId.of("UTC")),
            effectiveDate = LocalDate.of(2021, 5, 4),
            ledgerId = ledgerId,
            ledgerableType = null,
            ledgerableId = null,
            externalId = "zwt3-xxx123",
            liveMode = false
        )

        stubFor(
            get(urlMatching("/ledger_transactions/.+")).willReturn(
                ledgerTransactionResponse(
                    id.uuid,
                    ledgerId.uuid,
                    ledgerEntryId.uuid,
                    ledgerAccountId.uuid
                )
            )
        )
        val actualLedgerTransaction = client.getLedgerTransaction(LedgerTransactionId(UUID.randomUUID())).get()
        assertThat(actualLedgerTransaction).isEqualTo(expectedLedgerTransaction)
    }

    @Test
    fun `createLedgerTransaction request serialization`() {
        val ledgerAccountId = LedgerAccountId(UUID.randomUUID())
        val request = CreateLedgerTransactionRequest(
            LocalDate.of(2021, 5, 13),
            listOf(
                RequestLedgerEntry(
                    amount = 6,
                    direction = LedgerEntryDirection.DEBIT,
                    ledgerAccountId = ledgerAccountId,
                    lockVersion = 4
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
                  "ledger_account_id": {"ledger_account_uuid": "${ledgerAccountId.uuid}"},
                  "lock_version": 4
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
                .willReturn(
                    ledgerTransactionResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ledgerAccountId.uuid
                    )
                )
        )

        assertDoesNotThrow { client.createLedgerTransaction(request).get() }
    }

    @Test
    fun `updateLedgerTransaction makes a well-formed request`() {
        val uuid = UUID.randomUUID()
        val request = UpdateLedgerTransactionRequest(
            LedgerTransactionId(uuid),
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
            patch(urlMatching("/ledger_transactions/$uuid$")).withRequestBody(equalToJson(expectedRequestJson))
                .willReturn(
                    ledgerTransactionResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID()
                    )
                )
        )
        assertDoesNotThrow { client.updateLedgerTransaction(request).get() }
    }

    @Test
    fun `getLedgerTransaction builds the url path properly`() {
        val id = UUID.randomUUID()
        stubFor(
            get("/ledger_transactions/$id").willReturn(
                ledgerTransactionResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID()
                )
            )
        )
        assertDoesNotThrow { client.getLedgerTransaction(LedgerTransactionId(id)).get() }
    }

    @Test
    fun `getLedgerTransactions metadata serialization`() {
        val metadata = mapOf(
            "foo" to "bar",
            "fiz" to "buzz",
            "key with spaces" to "value with spaces",
        )
        stubFor(
            get(anyUrl())
                .withQueryParam("metadata%5Bfoo%5D", equalTo("bar"))
                .withQueryParam("metadata%5Bfiz%5D", equalTo("buzz"))
                /**
                 * There's a little bug in wiremock. query param values get their space characters decoded before
                 * comparing with the stub, so if we had tried to match a query param equalTo("value%20with%20spaces")
                 * then the test would fail. Even though we can clearly see in the netty logs that "value%20with%20spaces"
                 * is exactly what we're sending over the wire.
                 */
                .withQueryParam("metadata%5Bkey%20with%20spaces%5D", equalTo("value with spaces"))
                .willReturn(
                    ledgerTransactionsListResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID()
                    )
                        .withHeader("x-page", "1")
                        .withHeader("x-per-page", "3")
                        .withHeader("x-total-count", "40")
                )
        )

        assertDoesNotThrow { client.getLedgerTransactions(null, metadata).get() }
    }
}
