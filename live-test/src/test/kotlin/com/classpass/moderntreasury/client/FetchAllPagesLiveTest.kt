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
package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.containsOnly
import com.classpass.moderntreasury.ModernTreasuryLiveTest
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerEntryDirection
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchAllPagesLiveTest : ModernTreasuryLiveTest() {

    private lateinit var ledger: Ledger
    private lateinit var ledgerAccount: LedgerAccount
    private lateinit var debitAccount: LedgerAccount

    @BeforeAll
    fun setup() {
        ledger = client.createLedger(
            "client_liveTests_${this::class.simpleName}_${System.currentTimeMillis()}",
            null,
            "usd",
            nextId()
        ).get()
        ledgerAccount =
            client.createLedgerAccount("fetchAllPages-livetest-acct", null, NormalBalanceType.DEBIT, ledger.id, nextId()).get()
    }

    @AfterAll
    fun tearDown() {
        client.deleteLedger(ledger.id).get()
    }

    @Test
    fun `fetchAllPages fetches all pages`() {
        val transactions = List(5) { idx ->
            client.createLedgerTransaction(
                LocalDate.now(),
                listOf(
                    RequestLedgerEntry(123L, LedgerEntryDirection.CREDIT, ledgerAccount.id),
                    RequestLedgerEntry(123L, LedgerEntryDirection.DEBIT, ledgerAccount.id)
                ),
                "fetchAllPages-txn-$idx",
                null,
                LedgerTransactionStatus.PENDING,
                nextId(),
            )
        }.map { it.get() }

        val actual = client.fetchAllPages({ page, perPage ->
            getLedgerTransactions(
                ledgerAccountId = ledgerAccount.id,
                page = page,
                perPage = 1
            )
        }).get()
        assertThat(actual).containsOnly(*transactions.toTypedArray())
    }
}
