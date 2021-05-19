package com.classpass.moderntreasury.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A LedgerEntry represents an accounting entry within a parent LedgerTransaction. Its amount is denominated in the
 * currency of the ledger it belongs to.
 */
data class LedgerEntry(
    /**
     * Unique identifier for the ledger entry.
     */
    val id: String,
    /**
     * ID of the ledger account.
     */
    val ledgerAccountId: String,
    /**
     * Either CREDIT or DEBIT
     */
    val direction: LedgerEntryDirection,
    /**
     * Value in specified currency's smallest unit. e.g. $10 would be represented as 1000.
     */
    val amount: Long,
    /**
     * an integer that is incremented every time a transaction is posted to the account. When the client is creating a
     * ledger transaction and wants to assert that the ledger account's state hasn't changed since the last read, it can
     * optionally pass version into the create call for each ledger entry.
     */
    val lockVersion: Long?,
    /**
     * This field will be true if this object was created with a production API key or false if created with a test API
     * key.
     */
    val liveMode: Boolean
)

enum class LedgerEntryDirection {
    @JsonProperty("credit")
    CREDIT,
    @JsonProperty("debit")
    DEBIT
}
