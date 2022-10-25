/**
 * Copyright 2022 ClassPass
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
package com.classpass.moderntreasury.fake

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.client.fetchAllPages
import com.classpass.moderntreasury.exception.LedgerAccountVersionConflictException
import com.classpass.moderntreasury.exception.ModernTreasuryApiException
import com.classpass.moderntreasury.model.LedgerAccountId
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.request.DateQuery
import com.classpass.moderntreasury.model.request.InstantQuery
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ExecutionException

val CLOCK = Clock.systemUTC()
val NIK = "" // No idempotency key.
val TODAY = LocalDate.now(CLOCK)

class ModernTreasuryFakeTest {

    val client = ModernTreasuryFake()

    val usd = client.createLedger("USD", "", "USD", NIK).get()
    val can = client.createLedger("CAN", "", "CAN", NIK).get()

    val can_cash = client.createLedgerAccount("Cash", "", NormalBalanceType.CREDIT, can.id, NIK).get()
    val usd_cash = client.createLedgerAccount("Cash", "", NormalBalanceType.CREDIT, usd.id, NIK).get()
    val usd_cogs = client.createLedgerAccount("COGS", "", NormalBalanceType.DEBIT, usd.id, NIK).get()
    val us_venue = client.createLedgerAccount("US Venue", "", NormalBalanceType.CREDIT, usd.id, NIK).get()

    @BeforeEach
    fun setup() {
        client.clearAllTestTransactions()
    }

    @Test
    fun `Can create transactions and get balances`() {
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id)

        client.createLedgerTransaction(debit, credit)

        val cash = client.getLedgerAccount(usd_cash.id).get().balances.pendingBalance.amount
        assertEquals(-100L, cash)
    }

    @Test
    fun `getLedgerAccounts returns balances with balance as of date`() {
        val YESTERDAY = TODAY.minusDays(1)
        val TOMORROW = TODAY.plusDays(1)
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id)

        // Posting to BEFORE affects TODAY's balance.
        val balance1 = client.getLedgerAccount(us_venue.id, TODAY).get().balances.postedBalance.amount
        client.createLedgerTransaction(debit, credit, status = LedgerTransactionStatus.POSTED, effectiveDate = YESTERDAY)
        val balance2 = client.getLedgerAccount(us_venue.id, TODAY).get().balances.postedBalance.amount
        assertEquals(100, balance2 - balance1)

        // Posting to TODAY affects TODAY's balance.
        client.createLedgerTransaction(debit, credit, status = LedgerTransactionStatus.POSTED, effectiveDate = TODAY)
        val balance3 = client.getLedgerAccount(us_venue.id, TODAY).get().balances.postedBalance.amount
        assertEquals(100, balance3 - balance2)

        // Posting to TOMORROW has no effect.
        client.createLedgerTransaction(debit, credit, status = LedgerTransactionStatus.POSTED, effectiveDate = TOMORROW)
        val balance4 = client.getLedgerAccount(us_venue.id, TODAY).get().balances.postedBalance.amount
        assertEquals(0, balance4 - balance3)
    }

    @Test
    fun `getLedgerAccounts paginates correctly`() {
        // We're overriding perPage here to 2 so that we don't have to generate hundreds of rows of fake data to query for this test
        val page = client.getLedgerAccounts(listOf(us_venue.id, can_cash.id, usd_cogs.id), null, afterCursor = null, perPage = 2).get()
        assertThat(page.content).isEqualTo(listOf(us_venue, can_cash))
        assertThat(page.afterCursor).isEqualTo(can_cash.id.toString())
        assertThat(page.perPage).isEqualTo(2)
    }

    @Test
    fun `Updating transaction to posted affects posted balance`() {
        val debit1 = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit1 = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id)

        val ledgerTransaction =
            client.createLedgerTransaction(debit1, credit1, status = LedgerTransactionStatus.PENDING)

        client.updateLedgerTransaction(id = ledgerTransaction.id, status = LedgerTransactionStatus.POSTED).get()

        val cash = client.getLedgerAccount(usd_cash.id).get().balances.postedBalance.amount
        assertEquals(-100L, cash)
    }

    @Test
    fun `Can get transactions by ledgerId, ledgerAccountId, or metadata`() {
        val inUsd = client.createLedgerTransaction(
            description = "",
            effectiveDate = TODAY,
            externalId = "Automatic:${nextId++}",
            idempotencyKey = "",
            status = LedgerTransactionStatus.PENDING,
            metadata = mapOf("meta-key" to "good-value"),
            ledgerEntries = listOf(
                RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id),
                RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id),
            ),
        ).get()

        val viaCan = client.getLedgerTransactions(can.id).get().content.find { it.id == inUsd.id }
        assertNull(viaCan)

        val viaUsd = client.getLedgerTransactions(usd.id).get().content.find { it.id == inUsd.id }
        assertNotNull(viaUsd)

        val viaBadMeta = client.getLedgerTransactions(metadata = mapOf("meta-key" to "bad-value")).get().content.firstOrNull()
        assertNull(viaBadMeta)

        val viaMeta = client.getLedgerTransactions(metadata = mapOf("meta-key" to "good-value")).get().content.firstOrNull()
        assertNotNull(viaMeta)

        val viaLedgerAccountId = client.getLedgerTransactions(ledgerAccountId = usd_cash.id).get().content.find { it.id == inUsd.id }
        assertNotNull(viaLedgerAccountId)

        val viaBadLedgerAccountId = client.getLedgerTransactions(ledgerAccountId = LedgerAccountId(UUID.randomUUID())).get().content.find { it.id == inUsd.id }
        assertNull(viaBadLedgerAccountId)
    }

    @Test
    fun `Can get transactions by dates and times `() {
        val posted = client.createLedgerTransaction(
            description = "",
            effectiveDate = TODAY,
            externalId = "Automatic:${nextId++}",
            idempotencyKey = "",
            status = LedgerTransactionStatus.POSTED,
            metadata = mapOf("meta-key" to "good-value"),
            ledgerEntries = listOf(
                RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id),
                RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id),
            ),
        ).get()

        val pending = client.createLedgerTransaction(
            description = "",
            effectiveDate = TODAY,
            externalId = "Automatic:${nextId++}",
            idempotencyKey = "",
            status = LedgerTransactionStatus.PENDING,
            metadata = mapOf("meta-key" to "good-value"),
            ledgerEntries = listOf(
                RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id),
                RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id),
            ),
        ).get()

        val viaEffectiveDateHit = client.getLedgerTransactions(effectiveDate = DateQuery().greaterThanOrEqualTo(TODAY)).get().content.map { it.id }
        assertThat(viaEffectiveDateHit).containsAll(posted.id, pending.id)

        val viaEffectiveDateMiss = client.getLedgerTransactions(effectiveDate = DateQuery().lessThan(TODAY)).get().content.map { it.id }
        assertThat(viaEffectiveDateMiss).isEmpty()

        val viaPostedAtHit = client.getLedgerTransactions(postedAt = InstantQuery().lessThanOrEqualTo(Instant.now(CLOCK))).get().content.map { it.id }
        assertThat(viaPostedAtHit).containsOnly(posted.id)

        val viaPostedAtMiss = client.getLedgerTransactions(
            postedAt = InstantQuery().equalTo(
                Instant.now(CLOCK).plus(
                    Duration.ofMinutes(1)
                )
            )
        ).get().content.map { it.id }
        assertThat(viaPostedAtMiss).isEmpty()
    }

    @Test
    fun `Forbids unbalanced transactions`() {
        val oops = RequestLedgerEntry(90, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id)

        assertApiException("Transaction debits balance must equal credit balance") { client.createLedgerTransaction(oops, credit) }
    }

    @Test
    fun `Can not make a transaction across ledgers`() {
        val debit = RequestLedgerEntry(-100, LedgerEntryDirection.DEBIT, can_cash.id)
        val credit = RequestLedgerEntry(-100, LedgerEntryDirection.CREDIT, us_venue.id)

        assertApiException("Inconsistent Ledger Usage") { client.createLedgerTransaction(debit, credit) }
    }

    @Test
    fun `Ledger entries must have nonnegative amounts`() {
        val debit = RequestLedgerEntry(-100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(-100, LedgerEntryDirection.CREDIT, us_venue.id)

        assertApiException("Ledger entries must have nonnegative amounts") { client.createLedgerTransaction(debit, credit) }
    }

    @Test
    fun `Supports lockVersion`() {
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id, 0)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id, 0)
        client.createLedgerTransaction(debit, credit, status = LedgerTransactionStatus.POSTED)

        val cashVersion = client.getLedgerAccount(usd_cash.id).get()
        val venueVersion = client.getLedgerAccount(us_venue.id).get()

        val debit2 = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id, lockVersion = cashVersion.lockVersion)
        val credit2 = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id, lockVersion = venueVersion.lockVersion)
        client.createLedgerTransaction(debit2, credit2, status = LedgerTransactionStatus.POSTED)
    }

    @Test
    fun `An exception is thrown when creating a transaction with an outdated lock version`() {
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id, 0)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id, 0)

        // Will succeed because the lockVersion matches
        client.createLedgerTransaction(debit, credit, status = LedgerTransactionStatus.POSTED)

        try {
            // Should fail because the lock version increased when the previous ledger transaction was created
            client.createLedgerTransaction(debit, credit, status = LedgerTransactionStatus.POSTED)
            fail("Should have caught LedgerAccountVersionConflictException")
        } catch (ee: ExecutionException) {
            assertTrue(ee.cause is LedgerAccountVersionConflictException)
        }
    }

    @Test
    fun `Supports idempotent transaction creation`() {
        val KEY = "Supports idempotent transaction creation"
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id)

        val tx1 = client.createLedgerTransaction(
            TODAY,
            listOf(debit, credit),
            KEY,
            "",
            LedgerTransactionStatus.PENDING,
            KEY
        ).get()

        // Pretend we've lost the transaction and do it again.

        val tx2 = client.createLedgerTransaction(
            TODAY,
            listOf(debit, credit),
            KEY,
            "",
            LedgerTransactionStatus.PENDING,
            KEY
        ).get()

        assertEquals(tx1.id, tx2.id)
        val owe = client.getLedgerAccount(us_venue.id).get().balances.pendingBalance.amount
        assert(100L == owe)
    }

    @Test
    fun `fake fetchAllPages fetches all pages`() {
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, usd_cogs.id)

        val transactions = List(500) {
            client.createLedgerTransaction(
                TODAY,
                listOf(debit, credit),
                "",
                "",
                LedgerTransactionStatus.PENDING,
                "",
            ).get()
        }

        val result = client.fetchAllPages({ afterCursor, perPage ->
            getLedgerTransactions(
                ledgerAccountId = usd_cash.id,
                afterCursor = afterCursor,
                perPage = perPage
            )
        }).get()
        assertThat(result).isEqualTo(transactions)
    }

    @Test
    fun `fetchAllPages works on single-page result set`() {
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, usd_cogs.id)

        val transaction = client.createLedgerTransaction(
            TODAY,
            listOf(debit, credit),
            "",
            "",
            LedgerTransactionStatus.PENDING,
            "",
        ).get()

        val result = client.fetchAllPages({ afterCursor, perPage ->
            getLedgerTransactions(
                ledgerAccountId = usd_cash.id,
                afterCursor = afterCursor,
                perPage = perPage
            )
        }).get()
        assertThat(result).isEqualTo(listOf(transaction))
    }

    @Test
    fun `fetchAllPages works on empty result set`() {
        val result = client.fetchAllPages({ afterCursor, perPage ->
            getLedgerTransactions(
                ledgerAccountId = usd_cash.id,
                afterCursor = afterCursor,
                perPage = perPage
            )
        }).get()
        assertThat(result).isEqualTo(emptyList())
    }

    @Nested
    inner class FakeSpecificMethods {
        @Test
        fun `Can clearAllTestTransactions`() {
            val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id, 0)
            val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id, 0)
            client.createLedgerTransaction(debit, credit, status = LedgerTransactionStatus.POSTED)

            client.clearAllTestTransactions()
            client.createLedgerTransaction(debit, credit, status = LedgerTransactionStatus.POSTED)

            val balance = client.getLedgerAccount(us_venue.id).get().balances.postedBalance.amount
            assertEquals(100L, balance)
        }
    }

    @Nested
    inner class MetadataUpdating {
        @Test
        fun `setting key to null value clears metadata`() {
            assertNull(mapOf("my_key" to "my_value").updatedWith(mapOf("my_key" to null))["my_key"])
        }

        @Test
        fun `setting key to empty string value clears metadata`() {
            assertNull(mapOf("my_key" to "my_value").updatedWith(mapOf("my_key" to ""))["my_key"])
        }

        @Test
        fun `replaces old value when new value is specified for key`() {
            assertEquals(
                "new_value",
                mapOf("my_key" to "old_value").updatedWith(mapOf("my_key" to "new_value"))["my_key"]
            )
        }

        @Test
        fun `values for new keys are included in updated metadata`() {
            assertEquals("my_value", emptyMap<String, String>().updatedWith(mapOf("my_key" to "my_value"))["my_key"])
        }
    }
}

var nextId = 1L

/**
 * Assert a specific API exception result.
 *
 * Does not eat other errors.
 */
fun assertApiException(errorMessage: String, it: () -> Unit) {
    val exception = try {
        it()
    } catch (x: ExecutionException) {
        val cause = x.cause // "Smart cast to 'Throwable' is impossible, because 'x.cause' is a property that has open or custom getter"
        if (cause is ModernTreasuryApiException) cause
        else throw x
    } catch (x: ModernTreasuryApiException) { x }
        as? ModernTreasuryApiException

    assertTrue(exception != null, "Expected ModernTreasuryAPIException")
    assertEquals(exception?.errorMessage, errorMessage)
}

fun ModernTreasuryClient.createLedgerTransaction(
    vararg entries: RequestLedgerEntry,
    status: LedgerTransactionStatus = LedgerTransactionStatus.PENDING,
    effectiveDate: LocalDate = TODAY,
) =
    this.createLedgerTransaction(
        effectiveDate = effectiveDate,
        ledgerEntries = entries.toList(),
        externalId = "Automatic:${nextId++}",
        description = "",
        status = status,
        idempotencyKey = ""
    ).get()
