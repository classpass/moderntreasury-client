package com.classpass.moderntreasury.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

/**
 * A LedgerAccount is an account in a double-entry accounting system. Common examples include asset, liability, expense,
 * and revenue accounts. Each LedgerAccount belongs to a Ledger and can only have entries with other accounts belonging
 * to the same Ledger. API Doc reference: https://docs.moderntreasury.com/reference#ledger-account-object
 */
data class LedgerAccount(
    /**
     * Unique identifier for the ledger account.
     */
    val id: LedgerAccountId,
    /**
     * The name of the ledger account. e.g. Assets
     */
    val name: String,
    /**
     * An optional free-form description for internal use.
     */
    val description: String?,
    /**
     * Either DEBIT or CREDIT
     */
    val normalBalance: NormalBalanceType,
    /**
     * The ...
     */
    val balances: LedgerAccountBalances,
    /**
     * The ID of the ledger this account belongs to.
     */
    val ledgerId: LedgerId,
    /**
     * an integer that is incremented every time a transaction is posted to the account.
     */
    val lockVersion: Long,
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

/**
 * If an account is credit normal, then a "negative" balance would be one where the debit balance exceeds the credit
 * balance. For example, liabilities accounts are credit normal whereas assets accounts are debit normal.
 */
enum class NormalBalanceType {
    @JsonProperty("credit") CREDIT,
    @JsonProperty("debit") DEBIT
}

data class LedgerAccountId(
    val uuid: UUID
) {
    @JsonCreator
    constructor(uuidString: String) : this(UUID.fromString(uuidString))

    @JsonValue
    override fun toString() = uuid.toString()
}
