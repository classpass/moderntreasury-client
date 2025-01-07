/**
 * Copyright 2025 ClassPass
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
import assertk.assertions.isEqualTo
import com.classpass.moderntreasury.model.LedgerAccount
import com.classpass.moderntreasury.model.LedgerAccountBalanceItem
import com.classpass.moderntreasury.model.LedgerAccountBalances
import com.classpass.moderntreasury.model.LedgerId
import com.classpass.moderntreasury.model.NormalBalanceType
import com.classpass.moderntreasury.model.request.CreateLedgerAccountRequest
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.junit.jupiter.api.Test
import java.util.UUID

class LedgerAccountTests : WireMockClientTest() {

    @Test
    fun `createLedgerAccount makes a well-formatted request body and deserializes responses properly`() {
        val uuid = UUID.randomUUID()
        val request = CreateLedgerAccountRequest(
            "the_name",
            "the_description",
            NormalBalanceType.CREDIT,
            LedgerId(uuid),
            "idempotencykey"
        )
        val expectedRequestJson = """
            {
                "name": "the_name",
                "description": "the_description",
                "normal_balance": "credit",
                "ledger_id": "$uuid",
                "metadata": {}
            }
        """
        stubFor(
            post(urlMatching("/ledger_accounts"))
                .withRequestBody(equalToJson(expectedRequestJson))
                .willReturn(ledgerAccountResponse(uuid))
        )

        val actualResponse = client.createLedgerAccount(request).get()
        val expectedDeserialized = LedgerAccount(
            id = actualResponse.id,
            name = "Operating Bank Account",
            ledgerId = LedgerId(uuid),
            description = null,
            normalBalance = NormalBalanceType.DEBIT,
            lockVersion = 23,
            metadata = emptyMap(),
            liveMode = true,
            balances = LedgerAccountBalances(
                pendingBalance = LedgerAccountBalanceItem(6, 23, -17, "USD"),
                postedBalance = LedgerAccountBalanceItem(0, 11, -11, "USD")
            )
        )
        assertThat(actualResponse).isEqualTo(expectedDeserialized)
    }
}
