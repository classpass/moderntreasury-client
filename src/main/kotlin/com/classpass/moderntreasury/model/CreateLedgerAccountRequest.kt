package com.classpass.moderntreasury.model

data class CreateLedgerAccountRequest(
    val name: String,
    val description: String?,
    val normalBalance: NormalBalanceType,
    val ledgerId: String,
    val metadata: Map<String, String> = emptyMap()
)
