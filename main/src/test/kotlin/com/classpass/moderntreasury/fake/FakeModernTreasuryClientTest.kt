package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals

val CLOCK = Clock.systemUTC()
val NIK = "" // No idempotency key.
val TODAY = LocalDate.now(CLOCK)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeModernTreasuryClientTest {

    val client = FakeModernTreasuryClient(CLOCK)

    val usd = client.createLedger("USD", "", "USD", NIK).get()
    val can = client.createLedger("CAN", "", "CAN", NIK).get()

    val usd_cash = client.createLedgerAccount("Cash", "", NormalBalanceType.CREDIT, usd.id, NIK).get()
    val usd_cogs = client.createLedgerAccount("COGS", "", NormalBalanceType.DEBIT, usd.id, NIK).get()
    val us_venue = client.createLedgerAccount("US Venue", "", NormalBalanceType.CREDIT, usd.id, NIK).get()

    @BeforeEach fun clearAllTransactions() = client.clearAllTransactions()

    @Test
    fun `Can create transactions and get balances`() {
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id)

        val tx1 = client.createLedgerTransaction(debit, credit)

        val cash = client.getLedgerAccountBalance(usd_cash.id).get().pending[0].amount
        assertEquals(-100L, cash)
    }

    @Test
    fun `Forbids unbalanced transactions`() {
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val oops = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, us_venue.id)

        assertThrows<Exception> {
            client.createLedgerTransaction(debit, oops)
        }
    }

    @Disabled("Idempotent support not yet merger")
    @Test
    fun `Supports idempotent transaction creation`() {
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id)

        val tx1 = client.createLedgerTransaction(
            TODAY,
            listOf(debit, credit),
            "Supports idempotent transaction creation",
            "",
            LedgerTransactionStatus.PENDING,
            "Supports idempotent transaction creation"
        ).get()

        // Pretend we've lost the transaction and do it again.

        val tx2 = client.createLedgerTransaction(
            TODAY,
            listOf(debit, credit),
            "Supports idempotent transaction creation",
            "",
            LedgerTransactionStatus.PENDING,
            "Supports idempotent transaction creation"
        ).get()

        assertEquals(tx1.id, tx2.id)
        val owe = client.getLedgerAccountBalance(us_venue.id).get().pending[0].amount
        assert(100L == owe)
    }
}

var nextId = 1L

fun ModernTreasuryClient.createLedgerTransaction(vararg entries: RequestLedgerEntry) =
    this.createLedgerTransaction(
        TODAY,
        entries.toList(),
        "Automatic:${nextId++}",
        "",
        LedgerTransactionStatus.PENDING,
        ""
    ).get()
