package com.classpass.moderntreasury.model

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * A LedgerTransaction is a transaction between two LedgerAccounts. To create a LedgerTransaction, there must be at
 * least one CREDIT ledger entry and one DEBIT ledger entry. Additionally, the sum of all credit entry amounts must
 * equal the sum of all debit entry amounts. The LedgerTransaction is immutable once it has posted.
 */
data class LedgerTransaction(
    /**
     * Unique identifier for the ledger transaction.
     */
    val id: String,
    /**
     * An optional free-form description for internal use.
     */
    val description: String?,
    /**
     * One of pending, posted, or archived.
     */
    val status: LedgerTransactionStatus,
    /**
     * Additional data represented as key-value pairs. Both the key and value must be strings. See
     * https://docs.moderntreasury.com/reference#metadata.
     */
    val metadata: Map<String, String>,
    val ledgerEntries: List<LedgerEntry>,
    /**
     * The time on which the ledger transaction posted. This is null if the ledger transaction is pending.
     */
    val postedAt: ZonedDateTime?,
    /**
     * The date on which the ledger transaction happened for reporting purposes.
     */
    val effectiveDate: LocalDate,
    /**
     * The ID of the ledger this account belongs to.
     */
    val ledgerId: String,
    /**
     * If the ledger transaction can be reconciled to another object in Modern Treasury, the type will be populated here,
     * otherwise null.
     */
    val ledgerableType: LedgerableType?,
    /**
     * If the ledger transaction can be reconciled to another object in Modern Treasury, the id will be populated here,
     * otherwise null.
     */
    val ledgerableId: String?,
    /**
     * A unique string to represent the ledger transaction. Only one ledger transaction may have this ID in the ledger.
     */
    val externalId: String,
    /**
     * This field will be true if this object was created with a production API key or false if created with a test API
     * key.
     */
    val liveMode: Boolean
)

enum class LedgerTransactionStatus {
    PENDING,
    POSTED,
    ARCHIVED
}

enum class LedgerableType {
    PAYMENT_ORDER,
    EXPECTED_PAYMENT,
    PAPER_ITEM,
    RETURN
}
