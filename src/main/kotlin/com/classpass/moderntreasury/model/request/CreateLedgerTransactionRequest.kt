package com.classpass.moderntreasury.model.request

import com.classpass.moderntreasury.model.LedgerTransactionStatus
import java.time.LocalDate

data class CreateLedgerTransactionRequest(
    val effectiveDate: LocalDate,
    val ledgerEntries: List<RequestLedgerEntries>,
    val externalId: String,
    val description: String?,
    val status: LedgerTransactionStatus?,
    val metadata: Map<String, String> = emptyMap(),
) {
}
