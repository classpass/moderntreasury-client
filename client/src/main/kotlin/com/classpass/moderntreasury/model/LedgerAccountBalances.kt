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

data class LedgerAccountBalances(
    val pendingBalance: LedgerAccountBalanceItem,
    val postedBalance: LedgerAccountBalanceItem
)

data class LedgerAccountBalanceItem(
    val credits: Long,
    val debits: Long,
    /** The net of credits and debits in the account. If the account is credit normal, then this value will be negative
     * if debits exceeds credits. If the account is debit normal, then this value will be negative if credits exceeds
     * debits.
     */
    val amount: Long,
    /**
     * An ISO currency code. e.g. "usd"
     */
    val currency: String
)
