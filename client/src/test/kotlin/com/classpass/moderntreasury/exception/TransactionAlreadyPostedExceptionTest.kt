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
