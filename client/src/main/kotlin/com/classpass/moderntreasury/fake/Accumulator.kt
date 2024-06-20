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
package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.model.LedgerAccountBalanceItem
import com.classpass.moderntreasury.model.LedgerAccountBalances
import com.classpass.moderntreasury.model.LedgerAccountId
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerTransaction
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.NormalBalanceType

/**
 * Calculate a running tally of transactions across one ledger account.
 */
internal class Accumulator
constructor(val accountId: LedgerAccountId, val balanceType: NormalBalanceType) {

    var pendingDebits = 0L
    var postedDebits = 0L
    var pendingCredits = 0L
    var postedCredits = 0L

    fun balance(currency: String) =
        LedgerAccountBalances(
            pendingBalance = LedgerAccountBalanceItem(
                pendingCredits,
                pendingDebits,
                balanceType.balance(pendingDebits, pendingCredits),
                currency
            ),
            postedBalance = LedgerAccountBalanceItem(
                postedCredits,
                postedDebits,
                balanceType.balance(postedDebits, postedCredits),
                currency
            ),
        )

    fun add(transaction: LedgerTransaction) {
        if (transaction.status == LedgerTransactionStatus.ARCHIVED)
            return

        val entry = transaction.ledgerEntries.find { it.ledgerAccountId == accountId }
            ?: return

        val amount = entry.amount
        if (transaction.status == LedgerTransactionStatus.PENDING) {
            if (entry.direction == LedgerEntryDirection.CREDIT) pendingCredits += amount else pendingDebits += amount
        } else if (transaction.status == LedgerTransactionStatus.POSTED) {
            if (entry.direction == LedgerEntryDirection.CREDIT) postedCredits += amount else postedDebits += amount
        }
    }
}

/**
 * If an account is credit normal, then a "negative" balance would be one where the debit balance exceeds the credit balance.
 *
 * N.B. It seems potentially error-prone to have two similar arguments of the same type.
 */
private fun NormalBalanceType.balance(debits: Long, credits: Long) =
    if (this == NormalBalanceType.CREDIT) credits - debits else debits - credits
