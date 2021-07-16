package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.exception.LedgerAccountVersionConflictException
import com.classpass.moderntreasury.exception.ModernTreasuryApiException
import com.classpass.moderntreasury.exception.TransactionAlreadyPostedException
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountBalanceItem
import com.classpass.moderntreasury.model.LedgerAccountBalances
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
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.supplyAsync

open class ModernTreasuryFake :
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
        accounts.replaceAll { _, value -> value.copy(lockVersion = 0) }
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
        ledgers.remove(id)
    }

    override fun getLedgerAccount(ledgerAccountId: LedgerAccountId, balancesAsOfDate: LocalDate?): CompletableFuture<LedgerAccount> = supplyAsync {
        val balances = getBalances(ledgerAccountId)
        accounts[ledgerAccountId] = accounts[ledgerAccountId]?.copy(balances = balances) ?: throwApiException("Ledger Account Not Found")

        accounts[ledgerAccountId]!!
    }

    override fun getLedgerAccounts(
        ledgerAccountIds: List<LedgerAccountId>,
        balancesAsOfDate: LocalDate?,
        page: Int,
        perPage: Int
    ): CompletableFuture<ModernTreasuryPage<LedgerAccount>> = supplyAsync {
        val accounts = ledgerAccountIds.mapNotNull {
            accounts[it]?.copy(balances = getBalances(it, asOfDate = balancesAsOfDate))
        }
        val modernTreasuryPageInfo = object : ModernTreasuryPageInfo {
            override val page = page
            override val perPage = perPage
            override val totalCount = accounts.size
        }
        val content = accounts.drop(page * perPage).take(perPage)
        ModernTreasuryPage(modernTreasuryPageInfo, content)
    }

    override fun createLedgerAccount(request: CreateLedgerAccountRequest): CompletableFuture<LedgerAccount> = supplyAsync {
        val ledger = ledgers[request.ledgerId]
            ?: throwApiException("Ledger Not Found")

        val startingBalances = LedgerAccountBalances(
            LedgerAccountBalanceItem(0, 0, 0, ledger.currency),
            LedgerAccountBalanceItem(0, 0, 0, ledger.currency),
        )
        val account = request.reify(LedgerAccountId(makeId()), ledger.id, startingBalances)
        accounts[account.id] = account
        account
    }

    private fun getBalances(ledgerAccountId: LedgerAccountId, asOfDate: LocalDate? = null): LedgerAccountBalances {
        val account = accounts[ledgerAccountId]
            ?: throwApiException("Ledger Account Not Found")

        val ledger = ledgers[account.ledgerId]
            ?: throwApiException("Ledger Not Found")

        val tally = Accumulator(account.id, account.normalBalance)

        transactions
            .filter { transaction -> transaction.ledgerId == ledger.id } // Skip many transactions; Optional, however.
            .filter { transaction -> transaction.status != LedgerTransactionStatus.ARCHIVED }
            .forEach { transaction -> tally.add(transaction) }

        return tally.balance(ledger.currency)
    }

    override fun getLedgerTransaction(id: LedgerTransactionId): CompletableFuture<LedgerTransaction> {
        val transaction = transactions.find { it.id == id }
            ?: throwApiException("Transaction Not Found")
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
                return@supplyAsync if (it != null) it else throwApiException("Internal Error")
            }
        }

        if (request.externalId.length > 0)
            if (transactions.find { it.externalId == request.externalId } != null)
                throwApiException("Duplicate External ID")

        val metadata = request.metadata.filterNonNullValues()
        val status = request.status ?: LedgerTransactionStatus.PENDING
        val nowLocalTZ = ZonedDateTime.now()
        val postedAt = if (status != LedgerTransactionStatus.PENDING) nowLocalTZ else null

        // Use first entry to find the ledger.
        val ledgerAccountId1 = request.ledgerEntries.first().ledgerAccountId
        val ledgerAccount1 = accounts[ledgerAccountId1]
            ?: throwApiException("Ledger Account Not Found")

        val ledgerEntries = request.ledgerEntries
            .map {
                it.reify(LedgerEntryId(makeId()))
            }
            .also { it.validate() }

        val ledgerId1 = ledgerAccount1.ledgerId
        ledgerEntries.all { ledgerId1 == accounts[it.ledgerAccountId]?.ledgerId } || throwApiException("Inconsistent Ledger Usage")
        ledgerEntries.all { it.amount >= 0 } || throwApiException("Ledger entries must have nonnegative amounts")

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

        addTransaction(transaction)

        if (request.idempotencyKey.length > 0)
            transactionIdByIk[request.idempotencyKey] = transaction.id
        transaction
    }

    private fun addTransaction(transaction: LedgerTransaction) {

        val accountUpdates: MutableList<() -> Unit> = mutableListOf()

        // For a permanent (POSTED) transaction:
        // 1. Confirm the lock version is valid
        // 2. Increment lock version on all accounts in the transaction
        if (transaction.status == LedgerTransactionStatus.POSTED) {
            transaction.ledgerEntries.forEach {
                val ledgerAccount = accounts[it.ledgerAccountId]!!
                val existingLockVersion = ledgerAccount.lockVersion
                if (it.lockVersion != null && it.lockVersion != existingLockVersion) {
                    throwLedgerAccountVersionConflictException()
                }
                val incrementedLockVersion = existingLockVersion.plus(1)
                val updatedAccount = ledgerAccount.copy(lockVersion = incrementedLockVersion)
                accountUpdates.add {
                    accounts[it.ledgerAccountId] = updatedAccount
                }
            }
        }

        // Safe to update all the accounts now we know there were no lock version conflicts
        accountUpdates.forEach { it.invoke() }
        transactions.add(transaction)
    }

    override fun updateLedgerTransaction(request: UpdateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> = supplyAsync {
        val transaction = transactions.find { it.id == request.id }
            ?: throwApiException("Not Found")

        if (transaction.status != LedgerTransactionStatus.PENDING) {
            // Trying to update to POSTED/ARCHIVED when already POSTED/ARCHIVED
            if (request.status != LedgerTransactionStatus.PENDING) {
                throwTransactionAlreadyPostedException()
            }

            // Trying to update, while leaving in PENDING state, when already POSTED/ARCHIVED
            throwApiException("Invalid state, transaction is: ${transaction.status}")
        }

        val ledgerEntries = request.ledgerEntries
            ?.map { it.reify(LedgerEntryId(makeId())) }
            ?.also { it.validate() }

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
        addTransaction(updated)
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
        LedgerAccountBalances(
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

private fun makeId() = UUID.randomUUID()

@Suppress("UNCHECKED_CAST")
private fun RequestMetadata.filterNonNullValues() =
    this.filter { (_, v) -> v != null }.toMap() as Map<String, String>

fun throwApiException(message: String, parameter: String? = null): Nothing = throw ModernTreasuryApiException(400, null, null, message, parameter)
fun throwLedgerAccountVersionConflictException(): Nothing = throw LedgerAccountVersionConflictException()
fun throwTransactionAlreadyPostedException(): Nothing = throw TransactionAlreadyPostedException()

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

private fun CreateLedgerAccountRequest.reify(ledgerAccountId: LedgerAccountId, ledgerId: LedgerId, balances: LedgerAccountBalances) =
    LedgerAccount(ledgerAccountId, this.name, this.description, this.normalBalance, balances, ledgerId, lockVersion = 0, this.metadata.filterNonNullValues(), LIVEMODE)

private fun CreateLedgerRequest.reify(id: LedgerId) =
    Ledger(id, name, description, currency, metadata.filterNonNullValues(), LIVEMODE)

private fun RequestLedgerEntry.reify(id: LedgerEntryId) =
    LedgerEntry(id, ledgerAccountId, direction, amount, lockVersion, LIVEMODE)

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
        throwApiException("Transaction debits balance must equal credit balance", "entries")
    }
}
