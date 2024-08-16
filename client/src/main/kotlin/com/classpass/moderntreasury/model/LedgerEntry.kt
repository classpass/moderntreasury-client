/**
 * Copyright 2024 ClassPass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.classpass.moderntreasury.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

/**
 * A LedgerEntry represents an accounting entry within a parent LedgerTransaction. Its amount is denominated in the
 * currency of the ledger it belongs to.
 */
data class LedgerEntry(
    /**
     * Unique identifier for the ledger entry.
     */
    val id: LedgerEntryId,
    /**
     * ID of the ledger account.
     */
    val ledgerAccountId: LedgerAccountId,
    /**
     * Either CREDIT or DEBIT
     */
    val direction: LedgerEntryDirection,
    /**
     * Value in specified currency's smallest unit. e.g. $10 would be represented as 1000.
     */
    val amount: Long,
    /**
     * an integer that is incremented every time a transaction is posted to the account. When the client is creating a
     * ledger transaction and wants to assert that the ledger account's state hasn't changed since the last read, it can
     * optionally pass version into the create call for each ledger entry.
     */
    val lockVersion: Long?,
    /**
     * This field will be true if this object was created with a production API key or false if created with a test API
     * key.
     */
    val liveMode: Boolean,
    /**
     * Optional metadata associated with this ledger entry
     */
    val metadata: Map<String, String> = emptyMap()
)

enum class LedgerEntryDirection {
    @JsonProperty("credit")
    CREDIT,
    @JsonProperty("debit")
    DEBIT
}

data class LedgerEntryId(
    val uuid: UUID
) {
    @JsonCreator
    constructor(uuidString: String) : this(UUID.fromString(uuidString))

    @JsonValue
    override fun toString() = uuid.toString()
}
