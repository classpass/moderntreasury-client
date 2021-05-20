package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.exception.ModernTreasuryClientException
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountBalance
import com.classpass.moderntreasury.model.LedgerAccountBalanceItem
import com.classpass.moderntreasury.model.LedgerEntry
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerTransaction
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
import java.util.concurrent.CompletableFuture.failedFuture

class FakeClient
constructor(val clock: Clock) :
    ModernTreasuryClient {
    private val accounts: MutableMap<String, LedgerAccount> = mutableMapOf()
    private val ledgers: MutableMap<String, Ledger> = mutableMapOf()
    private val transactions: MutableList<LedgerTransaction> = mutableListOf()

    /**
     * For test purposes, permit to clear all transactions.
     */
    fun clearAllTransactions() {
        transactions.removeAll { true }
    }

    override fun createLedger(request: CreateLedgerRequest): CompletableFuture<Ledger> {
        val ledger = request.reify(makeId())
        ledgers[ledger.id] = ledger
        return completedFuture(ledger)
    }

    override fun createLedgerAccount(request: CreateLedgerAccountRequest): CompletableFuture<LedgerAccount> {
        val ledger = ledgers[request.ledgerId]
            ?: return failedFuture(ModernTreasuryClientException("Ledger Not Found"))

        val account = request.reify(makeId(), ledger.id)
        accounts[account.id] = account
        return completedFuture(account)
    }

    override fun getLedgerAccountBalance(ledgerAccountId: String, asOfDate: LocalDate?): CompletableFuture<LedgerAccountBalance> {
        val account = accounts[ledgerAccountId]
            ?: return failedFuture(ModernTreasuryClientException("Ledger Account Not Found"))

        val ledger = ledgers[account.ledgerId]
            ?: return failedFuture(ModernTreasuryClientException("Ledger Not Found"))

        val tally = Tally(account.id, account.normalBalance)

        transactions
            .filter { transaction -> transaction.ledgerId == ledger.id } // Skip many transactions; Optional, however.
            .filter { transaction -> transaction.status != LedgerTransactionStatus.ARCHIVED }
            .forEach { transaction -> tally.add(transaction) }

        return completedFuture(tally.balance(ledger.currency))
    }

    override fun getLedgerTransaction(id: String): CompletableFuture<LedgerTransaction> {
        val transaction = transactions.find { it.id == id }
            ?: return failedFuture(ModernTreasuryClientException("Transaction Not Found"))
        return completedFuture(transaction)
    }

    override fun getLedgerTransactions(ledgerId: String?, metadata: Map<String, String>): CompletableFuture<ModernTreasuryPage<LedgerTransaction>> {
        val content = transactions.filter {
            (ledgerId != null && it.ledgerId == ledgerId) || metadata matches it.metadata
        }
        return completedFuture(ModernTreasuryPage(PageInfo(0, content.size, content.size), content))
    }

    override fun createLedgerTransaction(request: CreateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> {
        val metadata = request.metadata.filterNonNullKeys()
        val status = request.status ?: LedgerTransactionStatus.PENDING
        val nowLocalTZ = ZonedDateTime.now()
        val postedAt = if (status != LedgerTransactionStatus.PENDING) nowLocalTZ else null
        val ledgerAccountId1 = request.ledgerEntries.first().ledgerAccountId
        val ledgerAccount1 = accounts[ledgerAccountId1]
            ?: return failedFuture(ModernTreasuryClientException("Ledger Account Not Found"))

        val ledgerEntries = request.ledgerEntries.map { it.reify(makeId()) }

        val debits = ledgerEntries.fold(0L) { sum, it -> sum + if (it.direction == LedgerEntryDirection.DEBIT) it.amount else 0 }
        val credits = ledgerEntries.fold(0L) { sum, it -> sum + if (it.direction == LedgerEntryDirection.CREDIT) it.amount else 0 }
        require(debits == credits)

        val transaction = LedgerTransaction(
            id = makeId(),
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
        return completedFuture(transaction)
    }

    override fun updateLedgerTransaction(request: UpdateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> {
        val transaction = transactions.find { it.id == request.id }
            ?: return failedFuture(ModernTreasuryClientException("Not Found"))

        if (transaction.status != LedgerTransactionStatus.PENDING)
            return failedFuture(ModernTreasuryClientException("Invalid State"))

        val ledgerEntries = request.ledgerEntries ?.map { it.reify(makeId()) }

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

        // TODO merge entries?
        // TODO validate the update

        transactions.remove(transaction)
        transactions.add(updated)
        TODO("Not yet implemented")
    }

    override fun ping(): CompletableFuture<Map<String, String>> =
        completedFuture(mapOf("fake" to "true"))

    // java.io.Closeable
    override fun close() {}
}

/**
 * Calculate a running tally of transactions across one ledger account.
 */
class Tally
constructor(val accountId: String, val balanceType: NormalBalanceType) {
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
            pending = listOf(LedgerAccountBalanceItem(pendingCredits, pendingDebits, balance.first, currency)),
            posted = listOf(LedgerAccountBalanceItem(postedCredits, postedDebits, balance.second, currency))
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

private fun makeId() = UUID.randomUUID().toString()

private fun RequestMetadata.filterNonNullKeys() =
    this.filter { (_, v) -> v != null }.toMap() as Map<String, String>

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

private fun CreateLedgerAccountRequest.reify(ledgerAccountId: String, ledgerId: String) =
    LedgerAccount(ledgerAccountId, this.name, this.description, this.normalBalance, ledgerId, this.metadata.filterNonNullKeys(), LIVEMODE)

private fun CreateLedgerRequest.reify(id: String) =
    Ledger(id, this.name, this.description, this.currency, this.metadata.filterNonNullKeys(), LIVEMODE)

private fun RequestLedgerEntry.reify(id: String) =
    LedgerEntry(id = id, ledgerAccountId = this.ledgerAccountId, direction = this.direction, amount = this.amount, liveMode = LIVEMODE)

/**
 * If an account is credit normal, then a "negative" balance would be one where the debit balance exceeds the credit balance.
 *
 * N.B. It seems potentially error-prone to have two similar arguments of the same type.
 */
fun NormalBalanceType.amount(debits: Long, credits: Long) =
    if (this == NormalBalanceType.CREDIT) credits - debits else debits - credits
