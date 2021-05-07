package com.classpass.moderntreasury.model.request

import com.classpass.moderntreasury.model.LedgerTransactionStatus
import java.time.LocalDate

data class CreateLedgerTransactionRequest(
    val effectiveDate: LocalDate,
    val ledgerEntries: List<RequestLedgerEntry>,
    val externalId: String,
    val description: String?,
    val status: LedgerTransactionStatus?,
    val metadata: RequestMetadata = emptyMap(),
)
