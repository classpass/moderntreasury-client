/**
 * Copyright ${year} ClassPass
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
import com.classpass.moderntreasury.model.Ledger
import com.classpass.moderntreasury.model.LedgerId
import com.classpass.moderntreasury.model.request.CreateLedgerRequest
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.junit.jupiter.api.Test
import java.util.UUID

class LedgerTests : WireMockClientTest() {
    @Test
    fun `createLedger request and response`() {
        val uuid = UUID.randomUUID()
        val request = CreateLedgerRequest(
            "ledger_name",
            "description",
            "eur",
            "asdf234"
        )

        val expectedRequestJson = """
        {
           "name" : "ledger_name",
           "description" : "description",
           "currency" : "eur",
           "metadata" : { }
         }
        """
        stubFor(
            post(urlMatching("/ledgers")).withRequestBody(equalToJson(expectedRequestJson))
                .willReturn(ledgerResponse(uuid))
        )

        val expectedLedger = Ledger(
            LedgerId(uuid),
            "Business Ledger",
            null,
            "USD",
            emptyMap(),
            true
        )

        val actualLedger = client.createLedger(request).get()

        assertThat(actualLedger).isEqualTo(expectedLedger)
    }
}
