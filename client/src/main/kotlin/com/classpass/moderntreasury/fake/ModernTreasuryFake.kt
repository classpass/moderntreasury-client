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
package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.client.ModernTreasuryClient
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
import com.classpass.moderntreasury.model.request.CreateLedgerAccountRequest
import com.classpass.moderntreasury.model.request.CreateLedgerRequest
import com.classpass.moderntreasury.model.request.CreateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.DatePreposition
import com.classpass.moderntreasury.model.request.DateQuery
import com.classpass.moderntreasury.model.request.InstantQuery
import com.classpass.moderntreasury.model.request.ModernTreasuryTemporalQuery
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import com.classpass.moderntreasury.model.request.RequestMetadata
import com.classpass.moderntreasury.model.request.TemporalQueryPart
import com.classpass.moderntreasury.model.request.UpdateLedgerTransactionRequest
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.time.temporal.Temporal
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
        val account = accounts[ledgerAccountId] ?: throwApiException("Ledger Account Not Found")
        val balances = getBalances(ledgerAccountId, balancesAsOfDate)
        account.copy(balances = balances)
    }

    override fun getLedgerAccounts(
        ledgerAccountIds: List<LedgerAccountId>,
        balancesAsOfDate: LocalDate?,
        afterCursor: String?,
        perPage: Int
    ): CompletableFuture<ModernTreasuryPage<LedgerAccount>> = supplyAsync {
        // Create a local copy of ledger account info with computed balance. Does not mutate shared.
        val accounts = ledgerAccountIds.mapNotNull {
            accounts[it]?.copy(balances = getBalances(it, asOfDate = balancesAsOfDate))
        }

        val accountsAfterCursor = accounts.takeLastWhile { afterCursor != it.id.toString() }

        val content = accountsAfterCursor.take(perPage)
        val nextAfterCursor =
            if (accountsAfterCursor.isEmpty()) null else content.lastOrNull()?.id.toString()

        val modernTreasuryPageInfo = object : ModernTreasuryPageInfo {
            override val afterCursor: String? = nextAfterCursor
            override val perPage = perPage
        }
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

    private fun getBalances(ledgerAccountId: LedgerAccountId, asOfDate: LocalDate?): LedgerAccountBalances {
        synchronized(this) {
            val account = accounts[ledgerAccountId]
                ?: throwApiException("Ledger Account Not Found")

            val ledger = ledgers[account.ledgerId]
                ?: throwApiException("Ledger Not Found")

            val tally = Accumulator(account.id, account.normalBalance)

            transactions
                .filter { transaction -> transaction.ledgerId == ledger.id } // Skip many transactions; Optional, however.
                .filter { transaction -> transaction.status != LedgerTransactionStatus.ARCHIVED }
                .filter { transaction -> asOfDate == null || transaction.effectiveDate <= asOfDate }
                .forEach { transaction -> tally.add(transaction) }

            return tally.balance(ledger.currency)
        }
    }

    override fun getLedgerTransaction(id: LedgerTransactionId): CompletableFuture<LedgerTransaction> {
        val transaction = transactions.find { it.id == id }
            ?: throwApiException("Transaction Not Found")
        return completedFuture(transaction)
    }

    override fun getLedgerTransactions(
        ledgerId: LedgerId?,
        ledgerAccountId: LedgerAccountId?,
        metadata: Map<String, String>,
        effectiveDate: DateQuery?,
        postedAt: InstantQuery?,
        updatedAt: InstantQuery?,
        afterCursor: String?,
        perPage: Int
    ): CompletableFuture<ModernTreasuryPage<LedgerTransaction>> {
        val filteredTransactions = transactions
            .filter { ledgerId == null || it.ledgerId == ledgerId }
            .filter { metadata.isEmpty() || metadata matches it.metadata }
            .filter { effectiveDate?.test(it.effectiveDate) ?: true }
            .filter { txn ->
                if (postedAt == null) {
                    true
                } else {
                    txn.postedAt?.let { txnPostedAt -> postedAt.test(txnPostedAt) } ?: false
                }
            }
            .filter { ledgerAccountId == null || it.ledgerEntries.map { entry -> entry.ledgerAccountId }.contains(ledgerAccountId) }

        val transactionsAfterCursor = filteredTransactions.takeLastWhile { afterCursor != it.id.toString() }

        val pageContent = transactionsAfterCursor.take(perPage)
        val nextAfterCursor =
            if (transactionsAfterCursor.isEmpty()) null else pageContent.lastOrNull()?.id.toString()

        val modernTreasuryPageInfo = object : ModernTreasuryPageInfo {
            override val afterCursor: String? = nextAfterCursor
            override val perPage = perPage
        }

        // updatedAt not currently implemented in client
        return completedFuture(ModernTreasuryPage(modernTreasuryPageInfo, pageContent))
    }

    override fun createLedgerTransaction(request: CreateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> = createLedgerTransaction(request, null)

    /**
     * ModernTreasuryFake only: create a ledger transaction with the option to override it postedAt timestamp
     */
    fun createLedgerTransaction(request: CreateLedgerTransactionRequest, postedAtOverride: Instant?) =
        supplyAsync {
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
            val nowLocalTZ = Instant.now()
            val postedAt = if (status != LedgerTransactionStatus.PENDING) postedAtOverride ?: nowLocalTZ else null

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
                ledgerEntries = ledgerEntries.toSet(),
                postedAt = postedAt,
                effectiveDate = request.effectiveDate,
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

    /**
     * From https://docs.moderntreasury.com/reference#update-ledger-transaction
     * "...For posted ledger transactions, only the metadata attribute can be updated."
     * Accordingly, identifying requests which are only changes to metadata is of interest.
     */
    private fun UpdateLedgerTransactionRequest.metadataOnly(): Boolean {
        return description == null && ledgerEntries == null && metadata.isNotEmpty()
    }

    override fun updateLedgerTransaction(request: UpdateLedgerTransactionRequest): CompletableFuture<LedgerTransaction> = supplyAsync {
        val transaction = transactions.find { it.id == request.id }
            ?: throwApiException("Not Found")

        if (transaction.status != LedgerTransactionStatus.PENDING && !request.metadataOnly()) {
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

        val metadata = transaction.metadata.updatedWith(request.metadata)

        val updated = transaction.copy(
            description = request.description ?: transaction.description,
            status = request.status ?: transaction.status,
            ledgerEntries = ledgerEntries?.toSet() ?: transaction.ledgerEntries,
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

private const val LIVEMODE = false

private fun makeId() = UUID.randomUUID()

fun Map<String, String>.updatedWith(
    requestMetadata: RequestMetadata
): Map<String, String> {
    // Overrides any previous values with values from the request. Removes any keys that have been set to null or empty
    // string.
    return (this + requestMetadata).filter { !it.value.isNullOrEmpty() }.filterNonNullValues()
}

private fun CreateLedgerAccountRequest.reify(ledgerAccountId: LedgerAccountId, ledgerId: LedgerId, balances: LedgerAccountBalances) =
    LedgerAccount(ledgerAccountId, this.name, this.description, this.normalBalance, balances, ledgerId, lockVersion = 0, this.metadata.filterNonNullValues(), LIVEMODE)

private fun CreateLedgerRequest.reify(id: LedgerId) =
    Ledger(id, name, description, currency, metadata.filterNonNullValues(), LIVEMODE)

private fun RequestLedgerEntry.reify(id: LedgerEntryId) =
    LedgerEntry(id, ledgerAccountId, direction, amount, lockVersion, LIVEMODE)

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

@Suppress("UNCHECKED_CAST")
private fun RequestMetadata.filterNonNullValues() =
    this.filter { (_, v) -> v != null }.toMap() as Map<String, String>

private fun <T : Temporal> ModernTreasuryTemporalQuery<T>.test(targetTemporal: T): Boolean =
    this.queryParts.all { it.test(targetTemporal) }

private fun <T : Temporal> TemporalQueryPart<T>.test(targetTemporal: T): Boolean {
    val chronoField = when (targetTemporal) {
        is LocalDate -> ChronoField.EPOCH_DAY
        is Instant -> ChronoField.INSTANT_SECONDS
        else -> throw IllegalStateException()
    }
    val targetEpoch = targetTemporal.getLong(chronoField)
    val queryEpoch = this.temporal.getLong(chronoField)
    return when (this.preposition) {
        DatePreposition.GREATER_THAN -> targetEpoch > queryEpoch
        DatePreposition.GREATER_THAN_OR_EQUAL_TO -> targetEpoch >= queryEpoch
        DatePreposition.LESS_THAN -> targetEpoch < queryEpoch
        DatePreposition.LESS_THAN_OR_EQUAL_TO -> targetEpoch <= queryEpoch
        DatePreposition.EQUAL_TO -> targetEpoch == queryEpoch
    }
}
