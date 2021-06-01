package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.classpass.moderntreasury.ModernTreasuryLiveTest
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.NormalBalanceType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LedgerAccountLiveTest : ModernTreasuryLiveTest() {
    private lateinit var ledger: Ledger

    @BeforeAll
    fun setup() {
        ledger = client.createLedger(
            "client_liveTests_${this::class.simpleName}_${System.currentTimeMillis()}",
            null,
            "usd",
            "${Math.random()}"
        ).get()
    }

    @AfterAll
    fun tearDown() {
        client.deleteLedger(ledger.id).get()
    }

    @Test
    fun `LedgerAccount creation and balance checking`() {
        val ledgerAccount =
            client.createLedgerAccount("crudtest", null, NormalBalanceType.CREDIT, ledger.id, "${Math.random()}").get()

        val queriedLedgerAccount = client.getLedgerAccount(ledgerAccount.id).get()

        assertThat(queriedLedgerAccount).isEqualTo(ledgerAccount)

        println(client.getLedgerAccountBalance(ledgerAccount.id).get())
    }
}
