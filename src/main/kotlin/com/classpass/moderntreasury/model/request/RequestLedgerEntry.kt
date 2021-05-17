package com.classpass.moderntreasury.model.request

import com.classpass.moderntreasury.model.LedgerEntryDirection

data class RequestLedgerEntry(
    val amount: Long,
    val direction: LedgerEntryDirection,
    val ledgerAccountId: String,
    /**
     * an integer that is incremented every time a transaction is posted to the account. When the client is creating a
     * account. When the client is creating a ledger transaction and wants to assert that the ledger account's state
     * hasn't changed since the last read, it can optionally pass version into the create call for each ledger entry.
     *
     * The request will fail with a HTTP 409 CONFLICT ModernTreasuryApiException if the provided version doesn't match
     * the current version of the associated account. If two requests to create a transaction happen simultaneously that
     * both try to take out a "lock" against the same account, only one request will succeed.
     */
    val lockVersion: Int? = null
)
