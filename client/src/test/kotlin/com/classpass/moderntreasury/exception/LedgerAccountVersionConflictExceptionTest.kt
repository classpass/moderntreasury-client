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
