package com.classpass.moderntreasury.model.request

import com.classpass.moderntreasury.model.LedgerTransactionStatus
import java.time.LocalDate

/**
 * A ledger_transaction is a transaction between two ledger accounts. To create a ledger transaction, there must be at
 * least one credit ledger entry and one debit ledger entry. Additionally, the sum of all credit entry amounts must
 * equal the sum of all debit entry amounts. The ledger transaction is immutable once it has posted.
 */
data class CreateLedgerTransactionRequest(
    val effectiveDate: LocalDate,
    val ledgerEntries: List<RequestLedgerEntry>,
    val externalId: String,
    val description: String?,
    val status: LedgerTransactionStatus?,
    override val idempotencyKey: String,
    val metadata: RequestMetadata = emptyMap(),
) : IdempotentRequest
