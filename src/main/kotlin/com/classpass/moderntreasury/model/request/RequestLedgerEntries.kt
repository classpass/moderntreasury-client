package com.classpass.moderntreasury.model.request

import com.classpass.moderntreasury.model.LedgerEntryDirection

data class RequestLedgerEntries(
    val amount: Long,
    val direction: LedgerEntryDirection,
    val ledgerAccountId: String
)