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
package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.classpass.moderntreasury.ModernTreasuryLiveTest
import com.classpass.moderntreasury.exception.ModernTreasuryApiException
import com.classpass.moderntreasury.exception.TransactionAlreadyPostedException
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.request.CreateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFails

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LedgerAccountLiveTest : ModernTreasuryLiveTest() {
    val BEFORE = LocalDate.parse("2021-03-01")
    val THEDAY = BEFORE.plusDays(1)
    val AFTER = THEDAY.plusDays(1)
    private lateinit var ledger: Ledger
    private lateinit var ledgerAccount: LedgerAccount

    @BeforeAll
    fun setup() {
        ledger = client.createLedger(
            "client_liveTests_${this::class.simpleName}_${System.currentTimeMillis()}",
            null,
            "usd",
            nextId()
        ).get()
        ledgerAccount =
            client.createLedgerAccount("crudtest", null, NormalBalanceType.CREDIT, ledger.id, nextId()).get()
    }

    @AfterAll
    fun tearDown() {
        client.deleteLedger(ledger.id).get()
    }

    @Test
    fun `Can getLedgerAccount and getLedgerAccountBalance`() {
        val queriedLedgerAccount = client.getLedgerAccount(ledgerAccount.id).get()

        // Can't compare lockVersion
        val expected = ledgerAccount.copy(lockVersion = 0)
        val actual = queriedLedgerAccount.copy(lockVersion = 0)
        assertThat(expected).isEqualTo(actual)
    }

    @Test
    fun `Can make pending transactions and post them`() {
        val metadata = mapOf("testName" to this::`Can make pending transactions and post them`.name)
        val transactionRequest = CreateLedgerTransactionRequest(
            LocalDate.now(),
            listOf(
                RequestLedgerEntry(123L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(123L, LedgerEntryDirection.DEBIT, ledgerAccount.id)
            ),
            "external-id-test",
            null,
            LedgerTransactionStatus.PENDING,
            nextId(),
            metadata
        )

        val transaction = client.createLedgerTransaction(transactionRequest).get()
        val queriedTransaction = client.getLedgerTransaction(transaction.id).get()
        val transactionList = client.getLedgerTransactions(ledger.id, null, metadata).get()

        assertThat(queriedTransaction).isEqualTo(transaction)
        assertThat(transactionList.totalCount).isEqualTo(1)
        assertThat(transactionList.content[0]).isEqualTo(transaction)

        client.updateLedgerTransaction(transaction.id, null, LedgerTransactionStatus.POSTED).get()
    }

    @Test
    fun `When posting already-posted transaction then throws TransactionAlreadyPostedException`() {
        val transactionRequest = CreateLedgerTransactionRequest(
            LocalDate.now(),
            listOf(
                RequestLedgerEntry(123L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(123L, LedgerEntryDirection.DEBIT, ledgerAccount.id)
            ),
            "double-post-test",
            null,
            LedgerTransactionStatus.POSTED,
            "${Math.random()}"
        )

        val txn = client.createLedgerTransaction(transactionRequest).get()
        val thrown =
            assertFails { client.updateLedgerTransaction(id = txn.id, status = LedgerTransactionStatus.POSTED).get() }
        assertThat(thrown.cause).isNotNull().isInstanceOf(TransactionAlreadyPostedException::class)
    }

    @Test
    fun `When posting already-archived transaction then throws TransactionAlreadyPostedException`() {
        val transactionRequest = CreateLedgerTransactionRequest(
            LocalDate.now(),
            listOf(
                RequestLedgerEntry(123L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(123L, LedgerEntryDirection.DEBIT, ledgerAccount.id)
            ),
            "update-archived-txn-test",
            null,
            LedgerTransactionStatus.ARCHIVED,
            "${Math.random()}"
        )

        val txn = client.createLedgerTransaction(transactionRequest).get()
        val thrown = assertFails {
            client.updateLedgerTransaction(id = txn.id, status = LedgerTransactionStatus.POSTED).get()
        }
        assertThat(thrown.cause).isNotNull().isInstanceOf(TransactionAlreadyPostedException::class)
    }

    @Test
    fun `Ledger entries may have zero-valued amounts`() {
        val account2 = client.createLedgerAccount("debits", null, NormalBalanceType.DEBIT, ledger.id, nextId()).get()

        val request = CreateLedgerTransactionRequest(
            LocalDate.now(),
            listOf(
                RequestLedgerEntry(0L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(0L, LedgerEntryDirection.DEBIT, account2.id),
            ),
            "Ledger entries may have zero-valued amounts",
            null,
            LedgerTransactionStatus.POSTED,
            nextId()
        )

        client.createLedgerTransaction(request).get()
        val balance = client.getLedgerAccount(account2.id).get().balances
        assertThat(balance.postedBalance.amount).isEqualTo(0)
    }

    @Test
    fun `Ledger entries must have nonnegative amounts`() {
        val debits = client.createLedgerAccount("debits", null, NormalBalanceType.DEBIT, ledger.id, nextId()).get()

        val request = CreateLedgerTransactionRequest(
            LocalDate.now(),
            listOf(
                RequestLedgerEntry(-1L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(-1L, LedgerEntryDirection.DEBIT, debits.id),
            ),
            "Ledger entries must have nonnegative amounts",
            null,
            LedgerTransactionStatus.POSTED,
            nextId()
        )

        val wrapped = assertFails { client.createLedgerTransaction(request).get() }
        val failure = wrapped as? ModernTreasuryApiException ?: wrapped.cause as? ModernTreasuryApiException
        assertThat(failure?.errorMessage).isEqualTo("Ledger entries must have nonnegative amounts")
    }

    @Test
    fun `Balance as-of date is inclusive`() {
        val debits = client.createLedgerAccount("debits", null, NormalBalanceType.DEBIT, ledger.id, nextId()).get()

        val request = CreateLedgerTransactionRequest(
            THEDAY,
            listOf(
                RequestLedgerEntry(1L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(1L, LedgerEntryDirection.DEBIT, debits.id),
            ),
            "Balance as-of date is inclusive",
            null,
            LedgerTransactionStatus.POSTED,
            nextId()
        )

        client.createLedgerTransaction(request).get()

        val before = client.getLedgerAccount(debits.id, balancesAsOfDate = BEFORE).get().balances
        val theDay = client.getLedgerAccount(debits.id, balancesAsOfDate = THEDAY).get().balances
        val after = client.getLedgerAccount(debits.id, balancesAsOfDate = AFTER).get().balances

        assertThat(before.postedBalance.amount).isEqualTo(0)
        assertThat(theDay.postedBalance.amount).isEqualTo(1)
        assertThat(after.postedBalance.amount).isEqualTo(1)
    }

    @Test
    fun `lockVersion is optional`() {
        val ledgerAccount2 = client.createLedgerAccount("debits", null, NormalBalanceType.DEBIT, ledger.id, nextId()).get()

        val request = CreateLedgerTransactionRequest(
            THEDAY,
            listOf(
                RequestLedgerEntry(1L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(1L, LedgerEntryDirection.DEBIT, ledgerAccount2.id),
            ),
            "lockVersion is optional 1",
            null,
            LedgerTransactionStatus.POSTED,
            nextId()
        )

        client.createLedgerTransaction(request).get()

        val request2 = CreateLedgerTransactionRequest(
            THEDAY,
            listOf(
                RequestLedgerEntry(1L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(1L, LedgerEntryDirection.DEBIT, ledgerAccount2.id),
            ),
            "lockVersion is optional 2",
            null,
            LedgerTransactionStatus.POSTED,
            nextId()
        )

        client.createLedgerTransaction(request2).get()
    }

    @Test
    fun `getLedgerAccounts returns paginated results`() {
        val account1 = client.createLedgerAccount("crudtest", null, NormalBalanceType.CREDIT, ledger.id, nextId()).get()
        val account2 = client.createLedgerAccount("crudtest", null, NormalBalanceType.CREDIT, ledger.id, nextId()).get()

        val response = client.getLedgerAccounts(
            ledgerAccountIds = listOf(account1.id, account2.id),
            page = 2,
            perPage = 1
        ).get()

        assertEquals(1, response.content.size)
        assertEquals(2, response.page)
        assertEquals(1, response.perPage)
        assertEquals(2, response.totalCount)
        assertEquals(account1.id, response.content[0].id)
    }

    @Test
    fun `getLedgerAccounts returns balances with balance as of date`() {
        val debits = client.createLedgerAccount("debits", null, NormalBalanceType.DEBIT, ledger.id, nextId()).get()

        client.createLedgerTransaction(
            CreateLedgerTransactionRequest(
                THEDAY,
                listOf(
                    RequestLedgerEntry(1L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                    RequestLedgerEntry(1L, LedgerEntryDirection.DEBIT, debits.id),
                ),
                "getLedgerAccounts returns balances with balance as of date",
                null,
                LedgerTransactionStatus.POSTED,
                nextId()
            )
        ).get()

        val before = client.getLedgerAccounts(listOf(debits.id), balancesAsOfDate = BEFORE)
            .get().content[0].balances
        val theDay = client.getLedgerAccounts(listOf(debits.id), balancesAsOfDate = THEDAY)
            .get().content[0].balances
        val after = client.getLedgerAccounts(listOf(debits.id), balancesAsOfDate = AFTER)
            .get().content[0].balances

        assertThat(before.postedBalance.amount).isEqualTo(0)
        assertThat(theDay.postedBalance.amount).isEqualTo(1)
        assertThat(after.postedBalance.amount).isEqualTo(1)
    }
}
