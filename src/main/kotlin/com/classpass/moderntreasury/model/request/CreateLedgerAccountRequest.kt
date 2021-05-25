package com.classpass.moderntreasury.model.request

import com.classpass.moderntreasury.model.NormalBalanceType
import java.util.UUID

data class CreateLedgerAccountRequest(
    val name: String,
    val description: String?,
    val normalBalance: NormalBalanceType,
    val ledgerId: UUID,
    override val idempotencyKey: String,
    val metadata: RequestMetadata = emptyMap()
) : IdempotentRequest
