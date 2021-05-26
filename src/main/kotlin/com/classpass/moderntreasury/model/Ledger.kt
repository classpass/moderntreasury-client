package com.classpass.moderntreasury.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

/**
 * A Ledger reqpresents a standard chart of ledger accounts.
 * API Doc reference: https://docs.moderntreasury.com/reference#ledger-object
 */
data class Ledger(
    /**
     * Unique identifier for the ledger.
     */
    val id: LedgerId,
    /**
     * The name of the ledger.
     */
    val name: String,
    /**
     * An optional free-form description for internal use.
     */
    val description: String?,
    /**
     * An ISO currency code in which all associated ledger entries are denominated. e.g. "usd"
     */
    val currency: String,
    /**
     * Additional data represented as key-value pairs. Both the key and value must be strings. See
     * https://docs.moderntreasury.com/reference#metadata.
     */
    val metadata: Map<String, String>,
    /**
     * This field will be true if this object was created with a production API key or false if created with a test API
     * key.
     */
    val liveMode: Boolean
)

data class LedgerId(
    @JsonProperty("ledger_uuid")
    val uuid: UUID
)
