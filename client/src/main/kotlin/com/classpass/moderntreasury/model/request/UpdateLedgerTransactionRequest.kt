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
