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
package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountId
import com.classpass.moderntreasury.model.LedgerId
import com.classpass.moderntreasury.model.LedgerTransaction
import com.classpass.moderntreasury.model.LedgerTransactionId
import com.classpass.moderntreasury.model.LedgerTransactionStatus
import com.classpass.moderntreasury.model.ModernTreasuryPage
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.request.CreateLedgerAccountRequest
import com.classpass.moderntreasury.model.request.CreateLedgerRequest
import com.classpass.moderntreasury.model.request.CreateLedgerTransactionRequest
import com.classpass.moderntreasury.model.request.DateQuery
import com.classpass.moderntreasury.model.request.InstantQuery
import com.classpass.moderntreasury.model.request.RequestLedgerEntry
import com.classpass.moderntreasury.model.request.RequestMetadata
import com.classpass.moderntreasury.model.request.UpdateLedgerTransactionRequest
import java.io.Closeable
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

interface ModernTreasuryClient : Closeable {

    fun createLedgerAccount(request: CreateLedgerAccountRequest): CompletableFuture<LedgerAccount>

    fun createLedgerAccount(
        name: String,
        description: String?,
        normalBalanceType: NormalBalanceType,
        ledgerId: LedgerId,
        idempotencyKey: String,
        metadata: RequestMetadata = emptyMap()
    ) = createLedgerAccount(
        CreateLedgerAccountRequest(
            name,
            description,
            normalBalanceType,
            ledgerId,
            idempotencyKey,
            metadata
        )
    )

    fun getLedgerAccount(
        ledgerAccountId: LedgerAccountId,
        /**
         * The date of the balance in local time. Defaults to today's date.
         */
        balancesAsOfDate: LocalDate? = null
    ): CompletableFuture<LedgerAccount>

    fun getLedgerAccounts(
        ledgerAccountIds: List<LedgerAccountId>,
        /**
         * The date of the balance in local time. Defaults to today's date.
         */
        balancesAsOfDate: LocalDate? = null,
        afterCursor: LedgerAccountId? = null,
        perPage: Int = 25
    ): CompletableFuture<ModernTreasuryPage<LedgerAccount>>

    fun getLedgerTransaction(
        /**
         * The ID of the ledger transaction
         */
        id: LedgerTransactionId
    ): CompletableFuture<LedgerTransaction>

    fun getLedgerTransactions(
        ledgerId: LedgerId? = null,
        /**
         * Returns ledger transactions by the presence of this ledger account on either side of the transaction
         */
        ledgerAccountId: LedgerAccountId? = null,
        /**
         * Key/Value metadata pairs to search transactions for.
         */
        metadata: Map<String, String> = emptyMap(),
        effectiveDate: DateQuery? = null,
        postedAt: InstantQuery? = null,
        updatedAt: InstantQuery? = null,
        afterCursor: LedgerTransactionId? = null,
        perPage: Int = 25
    ): CompletableFuture<ModernTreasuryPage<LedgerTransaction>>

    fun createLedgerTransaction(
        effectiveDate: LocalDate,
        ledgerEntries: List<RequestLedgerEntry>,
        externalId: String,
        description: String?,
        status: LedgerTransactionStatus?,
        idempotencyKey: String,
        metadata: RequestMetadata = emptyMap()
    ): CompletableFuture<LedgerTransaction> = createLedgerTransaction(
        CreateLedgerTransactionRequest(
            effectiveDate,
            ledgerEntries,
            externalId,
            description,
            status,
            idempotencyKey,
            metadata
        )
    )

    fun createLedgerTransaction(
        request: CreateLedgerTransactionRequest
    ): CompletableFuture<LedgerTransaction>

    /**
     * Update a ledger transaction. Only pending ledger transactions can be updated. Returns the updated
     * LedgerTransaction.
     */
    fun updateLedgerTransaction(
        /**
         * The ID of the ledger transaction to update
         */
        id: LedgerTransactionId,
        description: String? = null,
        /**
         * To post the ledger transaction, use POSTED. To archive a pending ledger transaction, use ARCHIVED.
         */
        status: LedgerTransactionStatus? = null,
        /**
         * Note that updating entries will overwrite any prior entries on the ledger transaction.
         */
        ledgerEntries: List<RequestLedgerEntry>? = null,
        metadata: RequestMetadata = emptyMap()
    ): CompletableFuture<LedgerTransaction> =
        updateLedgerTransaction(UpdateLedgerTransactionRequest(id, description, status, ledgerEntries, metadata))

    /**
     * Update a ledger transaction. Only pending ledger transactions can be updated. Returns the updated
     * LedgerTransaction.
     */
    fun updateLedgerTransaction(request: UpdateLedgerTransactionRequest): CompletableFuture<LedgerTransaction>

    /**
     * Set a pending ledger transaction's status to ARCHIVED, effectively a soft delete.
     */
    fun archiveLedgerTransaction(
        /**
         * The ID of the ledger transaction to archive
         */
        id: LedgerTransactionId,
    ): CompletableFuture<LedgerTransaction> = updateLedgerTransaction(
        id,
        null,
        LedgerTransactionStatus.ARCHIVED
    )

    fun createLedger(request: CreateLedgerRequest): CompletableFuture<Ledger>

    fun createLedger(
        /**
         * The name of the ledger.
         */
        name: String,
        /**
         * An optional free-form description for internal use.
         */
        description: String? = null,
        /**
         * An ISO currency code in which all associated ledger entries are denominated. e.g. "usd"
         */
        currency: String,
        idempotencyKey: String,
        /**
         * Additional data represented as key-value pairs. See https://docs.moderntreasury.com/reference#metadata.
         */
        metadata: RequestMetadata = emptyMap()
    ) = createLedger(CreateLedgerRequest(name, description, currency, idempotencyKey, metadata))

    /**
     * Delete a ledger. Deleting a ledger will delete all of its associated ledger accounts
     */
    fun deleteLedger(
        /**
         * The ID of the ledger to be deleted
         */
        id: LedgerId
    ): CompletableFuture<Unit>

    fun ping(): CompletableFuture<Map<String, String>>
}
