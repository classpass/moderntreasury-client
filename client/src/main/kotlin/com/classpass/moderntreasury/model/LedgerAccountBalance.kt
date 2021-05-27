package com.classpass.moderntreasury.model

data class LedgerAccountBalance(
    @Deprecated("Use pendingBalance")
    val pending: List<LedgerAccountBalanceItem>,
    @Deprecated("use postedBalance")
    val posted: List<LedgerAccountBalanceItem>,
    val pendingBalance: LedgerAccountBalanceItem,
    val postedBalance: LedgerAccountBalanceItem
)

data class LedgerAccountBalanceItem(
    val credits: Long,
    val debits: Long,
    /** The net of credits and debits in the account. If the account is credit normal, then this value will be negative
     * if debits exceeds credits. If the account is debit normal, then this value will be negative if credits exceeds
     * debits.
     */
    val amount: Long,
    /**
     * An ISO currency code. e.g. "usd"
     */
    val currency: String
)
