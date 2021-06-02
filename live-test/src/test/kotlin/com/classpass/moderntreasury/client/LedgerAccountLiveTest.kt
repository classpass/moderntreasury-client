package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.classpass.moderntreasury.ModernTreasuryLiveTest
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
import kotlin.test.assertFails

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LedgerAccountLiveTest : ModernTreasuryLiveTest() {
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
    fun `LedgerAccount retrieval and balance checking`() {
        val queriedLedgerAccount = client.getLedgerAccount(ledgerAccount.id).get()

        assertThat(queriedLedgerAccount).isEqualTo(ledgerAccount)

        println(client.getLedgerAccountBalance(ledgerAccount.id).get())
    }

    @Test
    fun `make transactions`() {
        val metadata = mapOf("testName" to this::`make transactions`.name)
        val transactionRequest = CreateLedgerTransactionRequest(
            LocalDate.now(),
            listOf(
                RequestLedgerEntry(123L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(123L, LedgerEntryDirection.DEBIT, ledgerAccount.id)
            ),
            "external-id-test",
            null,
            LedgerTransactionStatus.PENDING,
            com.classpass.moderntreasury.ModernTreasuryLiveTest.Companion.nextId(),
            metadata
        )

        val transaction = client.createLedgerTransaction(transactionRequest).get()
        val queriedTransaction = client.getLedgerTransaction(transaction.id)
        val transactionList = client.getLedgerTransactions(ledger.id, metadata)

        assertThat(queriedTransaction.get()).isEqualTo(transaction)
        assertThat(transactionList.get().totalCount).isEqualTo(1)
        assertThat(transactionList.get().content[0]).isEqualTo(transaction)

        client.updateLedgerTransaction(transaction.id, null, LedgerTransactionStatus.POSTED).get()
    }

    @Test
    fun `transaction already posted`() {
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
    fun `transaction already archived`() {
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
}
