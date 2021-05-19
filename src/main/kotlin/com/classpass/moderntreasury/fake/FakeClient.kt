package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.exception.ModernTreasuryClientException
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountBalance
import com.classpass.moderntreasury.model.LedgerEntry
import com.classpass.moderntreasury.model.LedgerTransaction
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.ModernTreasuryPage
import com.classpass.moderntreasury.model.ModernTreasuryPageInfo
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

class FakeClient : ModernTreasuryClient {
    constructor(clock: Clock) {
        this.clock = clock
    }
    val clock: Clock
    val accounts: MutableMap<String, LedgerAccount> = mutableMapOf()
    val ledgers: MutableMap<String, Ledger> = mutableMapOf()
    val transactions: MutableList<LedgerTransaction> = mutableListOf()

    override fun createLedger(request: CreateLedgerRequest): CompletableFuture<Ledger> {
        val ledger = request.reify(makeId())
        ledgers.put(ledger.id, ledger)
        return completedFuture(ledger)
    }

    override fun createLedgerAccount(request: CreateLedgerAccountRequest): CompletableFuture<LedgerAccount> {
        val ledger = ledgers[request.ledgerId]
            ?: return failedFuture(ModernTreasuryClientException("Not Found"))

        val account = request.reify(makeId(), ledger.id)
        accounts.put(account.id, account)
        return completedFuture(account)
    }

    override fun getLedgerAccountBalance(ledgerAccountId: String, asOfDate: LocalDate?): CompletableFuture<LedgerAccountBalance> {
        TODO("Not yet implemented")
    }

    override fun getLedgerTransaction(id: String): CompletableFuture<LedgerTransaction> {
        val transaction = transactions.find { it.id == id }
            ?: return failedFuture(ModernTreasuryClientException("Not Found"))
        return completedFuture(transaction)
    }

    override fun getLedgerTransactions(ledgerId: String?, metadata: Map<String, String>): CompletableFuture<ModernTreasuryPage<LedgerTransaction>> {
        val content = transactions.filter { it ->
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

        // TODO: validate the transaction.

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
        TODO("Not yet implemented")
    }

    override fun ping(): CompletableFuture<Map<String, String>> =
        completedFuture(mapOf("fake" to "true"))

    // java.io.Closeable
    override fun close() {}
}

private val LIVEMODE = false

private fun makeId() = UUID.randomUUID().toString()

private fun RequestMetadata.filterNonNullKeys() =
    this.filter { (k, v) -> v != null }.toMap() as Map<String, String>

/**
 * this matches other if all for all keys in this map, the value exists and matches in other.
 *
 * Note that (a matches b) does NOT imply (b matches a).
 */
private infix fun <K, V> Map<K, V>.matches(other: Map<K, V>) =
    this.all { (k, v) -> other.containsKey(k) && this[k] == other[k] }

private data class PageInfo(
    override val page: Int,
    override val perPage: Int,
    override val totalCount: Int
) : ModernTreasuryPageInfo

private fun CreateLedgerAccountRequest.reify(ledgerAccountId: String, ledgerId: String) =
    LedgerAccount(ledgerAccountId, this.name, this.description, this.normalBalance, ledgerId, this.metadata.filterNonNullKeys(), LIVEMODE)

private fun CreateLedgerRequest.reify(id: String) =
    Ledger(makeId(), this.name, this.description, this.currency, this.metadata.filterNonNullKeys(), LIVEMODE)

private fun RequestLedgerEntry.reify(id: String) =
    LedgerEntry(id = id, ledgerAccountId = this.ledgerAccountId, direction = this.direction, amount = this.amount, liveMode = LIVEMODE)
