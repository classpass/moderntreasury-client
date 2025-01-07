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
package com.classpass.moderntreasury.exception

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.classpass.moderntreasury.client.WireMockClientTest
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import org.junit.jupiter.api.Test
import kotlin.test.assertFails

class LedgerAccountVersionConflictExceptionTest : WireMockClientTest() {
    @Test
    fun `LedgerAccountVersionConflictException mapping test`() {
        val errorJson = """
            {
              "errors": {
                "code":"parameter_invalid",
                "message":"The ledger transaction write failed because at least one of the provided ledger account versions is incorrect",
                "parameter":"ledger_entries"
              }
            }
        """
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(422).withBody(errorJson)))

        val thrown = assertFails { client.ping().get() }
        assertThat(thrown.cause).isNotNull().isInstanceOf(LedgerAccountVersionConflictException::class)
    }
}
