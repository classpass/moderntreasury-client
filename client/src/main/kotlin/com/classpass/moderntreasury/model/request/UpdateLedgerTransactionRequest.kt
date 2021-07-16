package com.classpass.moderntreasury.model.request

import com.classpass.moderntreasury.model.LedgerTransactionId
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

data class UpdateLedgerTransactionRequest(
    /**
     * The ID of the ledger transaction to update
     */
    @JsonIgnore
    val id: LedgerTransactionId,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val description: String? = null,
    /**
     * To post the ledger transaction, use POSTED. To archive a pending ledger transaction, use ARCHIVED.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val status: LedgerTransactionStatus? = null,
    /**
     * Note that updating entries will overwrite any prior entries on the ledger transaction. If not null, this list
     * must be non-empty.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val ledgerEntries: List<RequestLedgerEntry>? = null,
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val metadata: RequestMetadata = emptyMap(),
)
