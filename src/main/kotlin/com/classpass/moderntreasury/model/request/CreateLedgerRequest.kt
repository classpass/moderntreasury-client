package com.classpass.moderntreasury.model.request

data class CreateLedgerRequest(
    /**
     * The name of the ledger.
     */
    val name: String,
    /**
     * An optional free-form description for internal use.
     */
    val description: String? = null,
    /**
     * An ISO currency code in which all associated ledger entries are denominated. e.g. "usd"
     */
    val currency: String,
    override val idempotencyKey: String,
    /**
     * Additional data represented as key-value pairs. See https://docs.moderntreasury.com/reference#metadata.
     */
    val metadata: RequestMetadata = emptyMap()
): IdempotentRequest
