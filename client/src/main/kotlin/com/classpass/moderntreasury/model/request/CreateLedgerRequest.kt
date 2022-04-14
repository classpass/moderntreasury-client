/**
 * Copyright 2022 ClassPass
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
package com.classpass.moderntreasury.model.request

/**
 * A Ledger reqpresents a standard chart of ledger accounts.
 * API Doc reference: https://docs.moderntreasury.com/reference#ledger-object
 */
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
) : IdempotentRequest
