/**
 * Copyright 2024 ClassPass
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
import com.classpass.moderntreasury.model.request.DateQuery
import com.classpass.moderntreasury.model.request.InstantQuery
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
import java.time.ZoneOffset
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
            ledgerEntries = setOf(
                LedgerEntry(
                    id = ledgerEntryId,
                    liveMode = false,
                    amount = 6,
                    direction = LedgerEntryDirection.CREDIT,
                    ledgerAccountId = ledgerAccountId,
                    lockVersion = null,
                    metadata = mapOf(
                        "entryKey" to "entryValue"
                    )
                ),
            ),
            postedAt = ZonedDateTime.of(2020, 10, 20, 19, 11, 7, 0, ZoneOffset.UTC).toInstant(),
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
                  "ledger_account_id": "$ledgerAccountId",
                  "metadata": {},
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
                .willReturn(ledgerTransactionResponse())
        )
        assertDoesNotThrow { client.updateLedgerTransaction(request).get() }
    }

    @Test
    fun `updateLedgerTransaction does not include null descriptions or status`() {
        val uuid = UUID.randomUUID()
        val request = UpdateLedgerTransactionRequest(
            LedgerTransactionId(uuid),
            description = null,
            status = null,
            metadata = mapOf("foo" to "bar")
        )
        val expectedRequestJson = """
            {
               "metadata" : {
                 "foo" : "bar"
               }
            }
        """
        stubFor(
            patch(urlMatching("/ledger_transactions/$uuid$")).withRequestBody(equalToJson(expectedRequestJson))
                .willReturn(ledgerTransactionResponse())
        )
        client.updateLedgerTransaction(request).get()
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

        assertDoesNotThrow { client.getLedgerTransactions(null, null, metadata).get() }
    }

    @Test
    fun `getLedgerTransactions effectiveDate query param serialization`() {
        // We're just testing serialization so its fine that this combination of date ranges is impossible
        val dateQuery: DateQuery = DateQuery().greaterThan(LocalDate.of(1987, 5, 13))
            .greaterThanOrEqualTo(LocalDate.of(1986, 10, 7))
            .lessThan(LocalDate.of(2021, 7, 28))
            .lessThanOrEqualTo(LocalDate.of(1997, 8, 29))
            .equalTo(LocalDate.of(2019, 3, 25))

        stubFor(
            get(anyUrl())
                .withQueryParam("effective_date%5Bgt%5D", equalTo("1987-05-13"))
                .withQueryParam("effective_date%5Bgte%5D", equalTo("1986-10-07"))
                .withQueryParam("effective_date%5Blt%5D", equalTo("2021-07-28"))
                .withQueryParam("effective_date%5Blte%5D", equalTo("1997-08-29"))
                .withQueryParam("effective_date%5Beq%5D", equalTo("2019-03-25"))
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

        assertDoesNotThrow { client.getLedgerTransactions(null, effectiveDate = dateQuery).get() }
    }

    @Test
    fun `getLedgerTransactions updatedAt and postedAt query param serialization`() {
        // We're just testing serialization so its fine that this combination of date ranges is impossible
        val instantQuery = InstantQuery().greaterThan(ZonedDateTime.of(1987, 5, 13, 0, 0, 0, 0, ZoneOffset.UTC).toInstant())
            .greaterThanOrEqualTo(ZonedDateTime.of(1986, 10, 7, 0, 0, 0, 0, ZoneOffset.UTC).toInstant())
            .lessThan(ZonedDateTime.of(2021, 7, 28, 0, 0, 0, 0, ZoneOffset.UTC).toInstant())
            .lessThanOrEqualTo(ZonedDateTime.of(1997, 8, 29, 0, 0, 0, 0, ZoneOffset.UTC).toInstant())
            .equalTo(ZonedDateTime.of(2019, 3, 25, 0, 0, 0, 0, ZoneOffset.UTC).toInstant())

        stubFor(
            get(anyUrl())
                .withQueryParam("updated_at%5Bgt%5D", equalTo("1987-05-13T00:00:00Z"))
                .withQueryParam("updated_at%5Bgte%5D", equalTo("1986-10-07T00:00:00Z"))
                .withQueryParam("updated_at%5Blt%5D", equalTo("2021-07-28T00:00:00Z"))
                .withQueryParam("updated_at%5Blte%5D", equalTo("1997-08-29T00:00:00Z"))
                .withQueryParam("updated_at%5Beq%5D", equalTo("2019-03-25T00:00:00Z"))
                .withQueryParam("posted_at%5Bgt%5D", equalTo("1987-05-13T00:00:00Z"))
                .withQueryParam("posted_at%5Bgte%5D", equalTo("1986-10-07T00:00:00Z"))
                .withQueryParam("posted_at%5Blt%5D", equalTo("2021-07-28T00:00:00Z"))
                .withQueryParam("posted_at%5Blte%5D", equalTo("1997-08-29T00:00:00Z"))
                .withQueryParam("posted_at%5Beq%5D", equalTo("2019-03-25T00:00:00Z"))
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

        assertDoesNotThrow { client.getLedgerTransactions(null, updatedAt = instantQuery, postedAt = instantQuery).get() }
    }
}
