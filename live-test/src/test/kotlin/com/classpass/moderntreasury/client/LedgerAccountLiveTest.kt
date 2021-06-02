package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.classpass.moderntreasury.ModernTreasuryLiveTest
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
            "${Math.random()}"
        ).get()
        ledgerAccount =
            client.createLedgerAccount("crudtest", null, NormalBalanceType.CREDIT, ledger.id, "${Math.random()}").get()
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
        val transactionRequest = CreateLedgerTransactionRequest(
            LocalDate.now(),
            listOf(
                RequestLedgerEntry(123L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                RequestLedgerEntry(123L, LedgerEntryDirection.DEBIT, ledgerAccount.id)
            ),
            "external-id-test",
            null,
            LedgerTransactionStatus.PENDING,
            "${Math.random()}"
        )

        val transaction = client.createLedgerTransaction(transactionRequest).get()
        val queriedTransaction = client.getLedgerTransaction(transaction.id)
        val transactionList = client.getLedgerTransactions(ledger.id)

        assertThat(queriedTransaction.get()).isEqualTo(transaction)
        assertThat(transactionList.get().totalCount).isEqualTo(1)
        assertThat(transactionList.get().content[0]).isEqualTo(transaction)
    }
}
