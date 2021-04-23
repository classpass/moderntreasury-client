package com.classpass.moderntreasury

import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.classpass.moderntreasury.config.ModernTreasuryModule

fun main(args: Array<String>) {
    val config = ModernTreasuryConfig(args[0], args[1], args[2])
    val module = ModernTreasuryModule()
}