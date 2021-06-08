package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.exception.LedgerAccountVersionConflictException
import com.classpass.moderntreasury.exception.ModernTreasuryApiException
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.ExecutionException

val CLOCK = Clock.systemUTC()
val NIK = "" // No idempotency key.
val TODAY = LocalDate.now(CLOCK)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModernTreasuryFakeTest {

    val client = ModernTreasuryFake(CLOCK)

    val usd = client.createLedger("USD", "", "USD", NIK).get()
    val can = client.createLedger("CAN", "", "CAN", NIK).get()

    val can_cash = client.createLedgerAccount("Cash", "", NormalBalanceType.CREDIT, can.id, NIK).get()
    val usd_cash = client.createLedgerAccount("Cash", "", NormalBalanceType.CREDIT, usd.id, NIK).get()
    val usd_cogs = client.createLedgerAccount("COGS", "", NormalBalanceType.DEBIT, usd.id, NIK).get()
    val us_venue = client.createLedgerAccount("US Venue", "", NormalBalanceType.CREDIT, usd.id, NIK).get()

    @BeforeEach fun clearAllTestTransactions() = client.clearAllTestTransactions()

    @Test
    fun `Can create transactions and get balances`() {
        val debit = RequestLedgerEntry(100, LedgerEntryDirection.DEBIT, usd_cash.id)
        val credit = RequestLedgerEntry(100, LedgerEntryDirection.CREDIT, us_venue.id)

        client.createLedgerTransaction(debit, credit)

        val cash = client.getLedgerAccountBalance(usd_cash.id).get().pendingBalance.amount
        assertEquals(-100L, cash)
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
        val owe = client.getLedgerAccountBalance(us_venue.id).get().pendingBalance.amount
        assert(100L == owe)
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
    status: LedgerTransactionStatus = LedgerTransactionStatus.PENDING
) =
    this.createLedgerTransaction(
        TODAY,
        entries.toList(),
        "Automatic:${nextId++}",
        "",
        status,
        ""
    ).get()
