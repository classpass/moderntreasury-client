/**
 * Copyright 2025 ClassPass
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
import com.fasterxml.jackson.annotation.JsonValue
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
    val uuid: UUID
) {
    @JsonCreator
    constructor(uuidString: String) : this(UUID.fromString(uuidString))

    @JsonValue
    override fun toString() = uuid.toString()
}
