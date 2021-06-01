package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.ModernTreasuryLiveTest
import org.junit.jupiter.api.Test

class LedgerCrudTest : ModernTreasuryLiveTest() {
    @Test
    fun createAndDeleteLedger() {
        val ledger = client.createLedger(
            "client_liveTests_${this::class.simpleName}_${System.currentTimeMillis()}",
            null,
            "usd",
            "${Math.random()}"
        ).get()
        client.deleteLedger(ledger.id).get()
    }
}
