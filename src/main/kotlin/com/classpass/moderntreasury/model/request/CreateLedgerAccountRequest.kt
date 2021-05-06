package com.classpass.moderntreasury.model.request

import com.classpass.moderntreasury.model.NormalBalanceType

data class CreateLedgerAccountRequest(
    val name: String,
    val description: String?,
    val normalBalance: NormalBalanceType,
    val ledgerId: String,
    val metadata: Map<String, String> = emptyMap()
)
