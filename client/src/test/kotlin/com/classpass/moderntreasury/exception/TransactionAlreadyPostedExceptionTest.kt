/**
 * Copyright 2022 ClassPass
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

class TransactionAlreadyPostedExceptionTest : WireMockClientTest() {
    @Test
    fun `TransactionAlreadyPosted mapping test`() {
        val errorJson = """
            {
              "errors": {
                "code":"parameter_invalid",
                "message":"The ledger transaction may not be updated once it is posted",
                "parameter":"ledger_transaction"
              }
            }
        """
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(422).withBody(errorJson)))

        val thrown = assertFails { client.ping().get() }
        assertThat(thrown.cause).isNotNull().isInstanceOf(TransactionAlreadyPostedException::class)
    }
}
