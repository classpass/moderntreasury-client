package com.classpass.moderntreasury.client

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToWithGivenProperties
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import com.classpass.moderntreasury.exception.MissingPaginationHeadersException
import com.classpass.moderntreasury.exception.ModernTreasuryApiException
import com.classpass.moderntreasury.model.LedgerAccountBalance
import com.classpass.moderntreasury.model.request.IdempotentRequest
import com.github.tomakehurst.wiremock.client.BasicCredentials
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class AsyncModernTreasuryClientTest : WireMockClientTest() {
    @Test
    fun `verify basic auth is on the request`() {
        stubFor(get(urlEqualTo("/ping")).willReturn(ok("""{"foo": "bar"}""")))

        client.ping().get()

        val expectedCredentials = BasicCredentials(ORG_ID, API_KEY)
        verify(getRequestedFor(urlEqualTo("/ping")).withBasicAuth(expectedCredentials))
    }

    @Test
    fun `Unsucessful responses are converted to ModernTreasuryException`() {
        val errorJson = """
            {
              "errors": {
                "code":"resource_not_found",
                "message":"Resource not found",
                "parameter":"id"
              }
            }
        """.trimIndent()
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(404).withBody(errorJson)))

        val expected = ModernTreasuryApiException(404, errorJson, "resource_not_found", "Resource not found", "id")
        assertThat { client.ping().get() }.isFailure()
            .transform { it.cause as ModernTreasuryApiException }
            .isEqualToWithGivenProperties(
                expected,
                ModernTreasuryApiException::httpStatus,
                ModernTreasuryApiException::httpResponseBody,
                ModernTreasuryApiException::code,
                ModernTreasuryApiException::message,
                ModernTreasuryApiException::parameter
            )
    }

    @Test
    fun `Unsucessful responses that don't conform to MT's error spec still return something useful`() {
        val errorJson = "Uh oh! This isn't the right error format"
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500).withBody(errorJson)))

        val expected = ModernTreasuryApiException(500, errorJson, null, null, null)
        assertThat { client.ping().get() }.isFailure()
            .transform { it.cause as ModernTreasuryApiException }
            .isEqualToWithGivenProperties(
                expected,
                ModernTreasuryApiException::httpStatus,
                ModernTreasuryApiException::code,
                ModernTreasuryApiException::message,
                ModernTreasuryApiException::parameter
            )
    }

    @Test
    fun `post requests include idempotency key`() {
        val expectedIdempotencyKey = "Hello, I am the idempotency key"

        val request = object : IdempotentRequest {
            override val idempotencyKey = expectedIdempotencyKey
        }

        stubFor(
            post(anyUrl()).withHeader("Idempotency-Key", equalTo(expectedIdempotencyKey))
                .willReturn(ledgerAccountBalanceResponse)
        )
        assertDoesNotThrow { client.post<LedgerAccountBalance>("/foo", request).get() }
    }

    @Test
    fun `test paginated response deserialization`() {
        stubFor(
            get(anyUrl()).willReturn(
                ledgerTransactionsListResponse
                    .withHeader("x-page", "1")
                    .withHeader("x-per-page", "3")
                    .withHeader("x-total-count", "40")
            )
        )

        val result = client.getLedgerTransactions("foo").get()
        assertThat(result.page).isEqualTo(1)
        assertThat(result.perPage).isEqualTo(3)
        assertThat(result.totalCount).isEqualTo(40)
        assertThat(result.content).hasSize(3)
    }

    @Test
    fun `test missing pagination headers`() {
        stubFor(get(anyUrl()).willReturn(ledgerTransactionsListResponse))

        assertThat { client.getLedgerTransactions("foo").get() }.isFailure()
            .transform { it.cause!! }
            .isInstanceOf(MissingPaginationHeadersException::class.java)
    }
}
