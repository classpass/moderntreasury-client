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

import com.classpass.moderntreasury.model.LedgerAccountId
import com.classpass.moderntreasury.model.LedgerEntryDirection

data class RequestLedgerEntry(
    val amount: Long,
    val direction: LedgerEntryDirection,
    val ledgerAccountId: LedgerAccountId,
    /**
     * an integer that is incremented every time a transaction is posted to the account. When the client is creating a
     * account. When the client is creating a ledger transaction and wants to assert that the ledger account's state
     * hasn't changed since the last read, it can optionally pass version into the create call for each ledger entry.
     *
     * The request will fail with a LedgerAccountVersionConflictException if the provided version doesn't match
     * the current version of the associated account. If two requests to create a transaction happen simultaneously that
     * both try to take out a "lock" against the same account, only one request will succeed.
     */
    val lockVersion: Long? = null
)
