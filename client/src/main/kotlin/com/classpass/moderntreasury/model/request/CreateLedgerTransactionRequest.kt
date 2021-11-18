/**
 * Copyright 2021 ClassPass
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
