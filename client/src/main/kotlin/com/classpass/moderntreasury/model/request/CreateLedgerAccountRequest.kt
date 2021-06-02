package com.classpass.moderntreasury.model.request

import com.classpass.moderntreasury.model.LedgerId
import com.classpass.moderntreasury.model.NormalBalanceType

data class CreateLedgerAccountRequest(
    val name: String,
    val description: String?,
    val normalBalance: NormalBalanceType,
    val ledgerId: LedgerId,
    override val idempotencyKey: String,
    val metadata: RequestMetadata = emptyMap()
) : IdempotentRequest