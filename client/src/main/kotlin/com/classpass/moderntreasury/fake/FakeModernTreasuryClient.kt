package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.exception.ModernTreasuryApiException
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountBalance
import com.classpass.moderntreasury.model.LedgerAccountBalanceItem
import com.classpass.moderntreasury.model.LedgerAccountId
import com.classpass.moderntreasury.model.LedgerEntry
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerEntryId
import com.classpass.moderntreasury.model.LedgerId
import com.classpass.moderntreasury.model.LedgerTransaction
import com.classpass.moderntreasury.model.LedgerTransactionId
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.ModernTreasuryPage
import com.classpass.moderntreasury.model.ModernTreasuryPageInfo
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.request.CreateLedgerAccountRequest
import com.classpass.moderntreasury.model.request.CreateLedgerRequest
import com.classpass.moderntreasury.model.request.CreateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import com.classpass.moderntreasury.model.request.RequestMetadata
import com.classpass.moderntreasury.model.request.UpdateLedgerTransactionRequest
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.supplyAsync

class FakeModernTreasuryClient
constructor(val clock: Clock) :
    ModernTreasuryClient {
    private val accounts: MutableMap<LedgerAccountId, LedgerAccount> = mutableMapOf()
    private val ledgers: MutableMap<LedgerId, Ledger> = mutableMapOf()
    private val transactions: MutableList<LedgerTransaction> = mutableListOf()

    // Map idempotency key to actual id.
    private val transactionIdByIk = mutableMapOf<String, LedgerTransactionId>()

    /* Test API */
    fun clearAllTestTransactions() {
        transactions.clear()
        transactionIdByIk.clear()
    }

    /* Test API */
    fun clearAllTestLedgers() {
        ledgers.clear()
        accounts.clear()
        clearAllTestTransactions()
    }

    /* Test API */
    fun getTestLedgers(ledgerIds: List<LedgerId>): List<Ledger> {
        return ledgers.filter { ledgerIds.contains(it.key) }.values.toList()
    }

    override fun createLedger(request: CreateLedgerRequest): CompletableFuture<Ledger> = supplyAsync {
        val ledger = request.reify(LedgerId(makeId()))
        ledgers[ledger.id] = ledger
        ledger
    }

    override fun deleteLedger(id: LedgerId): CompletableFuture<Unit> = supplyAsync {
        ledgers.remove(id) ?: fail("Ledger Not Found")
    }

    override fun getLedgerAccount(ledgerAccountId: LedgerAccountId) = supplyAsync {
        accounts[ledgerAccountId] ?: fail("Ledger Account Not Found")
    }

    override fun createLedgerAccount(request: CreateLedgerAccountRequest): CompletableFuture<LedgerAccount> = supplyAsync {
        val ledger = ledgers[request.ledgerId]
            ?: fail("Ledger Not Found")

        val account = request.reify(LedgerAccountId(makeId()), ledger.id, LOCKVERSION)
        accounts[account.id] = account
        account
    }

    override fun getLedgerAccountBalance(ledgerAccountId: LedgerAccountId, asOfDate: LocalDate?): CompletableFuture<LedgerAccountBalance> = supplyAsync {
        val account = accounts[ledgerAccountId]
            ?: fail("Ledger Account Not Found")

        val ledger = ledgers[account.ledgerId]
            ?: fail("Ledger Not Found")

        val tally = Accumulator(account.id, account.normalBalance)

        transactions
            .filter { transaction -> transaction.ledgerId == ledger.id } // Skip many transactions; Optional, however.
            .filter { transaction -> transaction.status != LedgerTransactionStatus.ARCHIVED }
            .forEach { transaction -> tally.add(transaction) }

        tally.balance(ledger.currency)
    }

    override fun getLedgerTransaction(id: LedgerTransactionId): CompletableFuture<LedgerTransaction> {
        val transaction = transactions.find { it.id == id }
            ?: fail("Transaction Not Found")
        return completedFuture(transaction)
    }

    override fun getLedgerTransactions(ledgerId: LedgerId?, metadata: Map<String, String>): CompletableFuture<ModernTreasuryPage<LedgerTransaction>> {
        val content = transactions.filter {
            (ledgerId != null && it.ledgerId == ledgerId) || metadata matches it.metadata
        }
        return completedFuture(ModernTreasuryPage(PageInfo(0, content.size, content.size), content))
    }

    override fun createLedgerTransaction(request: CreateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> = supplyAsync {
        // Support idempotent requests.
        if (request.idempotencyKey.length > 0) {
            if (request.idempotencyKey in transactionIdByIk) {
                val id = transactionIdByIk[request.idempotencyKey]!!
                val it = transactions.find { it.id == id }
                return@supplyAsync if (it != null) it else fail("Internal Error")
            }
        }

        if (request.externalId.length > 0)
            if (transactions.find { it.externalId == request.externalId } != null)
                fail("Duplicate External ID")

        val metadata = request.metadata.filterNonNullValues()
        val status = request.status ?: LedgerTransactionStatus.PENDING
        val nowLocalTZ = ZonedDateTime.now()
        val postedAt = if (status != LedgerTransactionStatus.PENDING) nowLocalTZ else null

        // Use first entry to find the ledger.
        val ledgerAccountId1 = request.ledgerEntries.first().ledgerAccountId
        val ledgerAccount1 = accounts[ledgerAccountId1]
            ?: fail("Ledger Account Not Found")

        val ledgerEntries = request.ledgerEntries.map { it.reify(LedgerEntryId(makeId()), LOCKVERSION) }.also { it.validate() }

        val ledgerId1 = ledgerAccount1.ledgerId
        ledgerEntries.all { ledgerId1 == accounts[it.ledgerAccountId]?.ledgerId } || fail("Inconsistent Ledger Usage")
        ledgerEntries.all { it.amount >= 0 } || fail("Non-Negative Amounts") // MIGHT not be correct.

        val transaction = LedgerTransaction(
            id = LedgerTransactionId(makeId()),
            description = request.description,
            status = status,
            metadata = metadata,
            ledgerEntries = ledgerEntries,
            postedAt = postedAt,
            effectiveDate = nowLocalTZ.toLocalDate(),
            ledgerId = ledgerAccount1.ledgerId,
            ledgerableType = null,
            ledgerableId = null,
            request.externalId,
            LIVEMODE
        )

        transactions.add(transaction)
        if (request.idempotencyKey.length > 0)
            transactionIdByIk[request.idempotencyKey] = transaction.id
        transaction
    }

    override fun updateLedgerTransaction(request: UpdateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> = supplyAsync {
        val transaction = transactions.find { it.id == request.id }
            ?: fail("Not Found")

        if (transaction.status != LedgerTransactionStatus.PENDING)
            fail("Invalid State")

        val ledgerEntries = request.ledgerEntries?.map { it.reify(LedgerEntryId(makeId()), LOCKVERSION) }?.also { it.validate() }

        val metadata = transaction.metadata
            // Remove entries which are specifically set to null in the request.
            .filterNot { request.metadata.containsKey(it.key) && request.metadata[it.key] == null }
            // Override previous values with values from the request.
            .mapValues { (k, _) -> request.metadata[k] ?: transaction.metadata[k]!! }

        val updated = transaction.copy(
            description = request.description ?: transaction.description,
            status = request.status ?: transaction.status,
            ledgerEntries = ledgerEntries ?: transaction.ledgerEntries,
            metadata = metadata
        )

        transactions.remove(transaction)
        transactions.add(updated)
        updated
    }

    override fun ping(): CompletableFuture<Map<String, String>> =
        completedFuture(mapOf("fake" to "true"))

    // java.io.Closeable
    override fun close() {}
}

/**
 * Calculate a running tally of transactions across one ledger account.
 */
class Accumulator
constructor(val accountId: LedgerAccountId, val balanceType: NormalBalanceType) {
    var pendingDebits = 0L
    var postedDebits = 0L
    var pendingCredits = 0L
    var postedCredits = 0L

    fun balance(): Pair<Long, Long> {
        return Pair(
            balanceType.amount(pendingDebits, pendingCredits),
            balanceType.amount(postedDebits, postedCredits)
        )
    }

    fun balance(currency: String) = balance().let { balance ->
        LedgerAccountBalance(
            pendingBalance = LedgerAccountBalanceItem(pendingCredits, pendingDebits, balance.first, currency),
            postedBalance = LedgerAccountBalanceItem(postedCredits, postedDebits, balance.second, currency)
        )
    }

    fun add(transaction: LedgerTransaction) {
        if (transaction.status == LedgerTransactionStatus.ARCHIVED)
            return

        val entry = transaction.ledgerEntries.find { it.ledgerAccountId == accountId }
            ?: return

        val amount = entry.amount
        if (transaction.status == LedgerTransactionStatus.PENDING) {
            if (entry.direction == LedgerEntryDirection.CREDIT) pendingCredits += amount else pendingDebits += amount
        } else if (transaction.status == LedgerTransactionStatus.POSTED) {
            if (entry.direction == LedgerEntryDirection.CREDIT) postedCredits += amount else postedDebits += amount
        }
    }
}

private const val LIVEMODE = false
private const val LOCKVERSION = 0L

private fun makeId() = UUID.randomUUID()

@Suppress("UNCHECKED_CAST")
private fun RequestMetadata.filterNonNullValues() =
    this.filter { (_, v) -> v != null }.toMap() as Map<String, String>

fun fail(message: String, parameter: String? = null): Nothing = throw ModernTreasuryApiException(400, null, null, message, parameter)

/**
 * this matches other if all for all keys in this map, the value exists and matches in other.
 *
 * Note that (a matches b) does NOT imply (b matches a).
 */
private infix fun <K, V> Map<K, V>.matches(other: Map<K, V>) =
    this.all { (k, _) -> other.containsKey(k) && this[k] == other[k] }

private data class PageInfo(
    override val page: Int,
    override val perPage: Int,
    override val totalCount: Int
) : ModernTreasuryPageInfo

private fun CreateLedgerAccountRequest.reify(ledgerAccountId: LedgerAccountId, ledgerId: LedgerId, lockVersion: Long) =
    LedgerAccount(ledgerAccountId, this.name, this.description, this.normalBalance, ledgerId, lockVersion, this.metadata.filterNonNullValues(), LIVEMODE)

private fun CreateLedgerRequest.reify(id: LedgerId) =
    Ledger(id, this.name, this.description, this.currency, this.metadata.filterNonNullValues(), LIVEMODE)

private fun RequestLedgerEntry.reify(id: LedgerEntryId, lockVersion: Long) =
    LedgerEntry(id, this.ledgerAccountId, this.direction, this.amount, lockVersion, LIVEMODE)

/**
 * If an account is credit normal, then a "negative" balance would be one where the debit balance exceeds the credit balance.
 *
 * N.B. It seems potentially error-prone to have two similar arguments of the same type.
 */
fun NormalBalanceType.amount(debits: Long, credits: Long) =
    if (this == NormalBalanceType.CREDIT) credits - debits else debits - credits

/**
 * Ensure a list of ledger entries in a transaction is valid by requiring the credit balance to equal the debit balance
 */
private fun List<LedgerEntry>.validate() {
    val debits = fold(0L) { sum, it -> sum + if (it.direction == LedgerEntryDirection.DEBIT) it.amount else 0 }
    val credits = fold(0L) { sum, it -> sum + if (it.direction == LedgerEntryDirection.CREDIT) it.amount else 0 }
    if (debits != credits) {
        throw ModernTreasuryApiException(400, null, null, "Transaction debits balance must equal credit balance", "entries")
    }
}