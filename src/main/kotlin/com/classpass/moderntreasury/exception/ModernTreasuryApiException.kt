package com.classpass.moderntreasury.exception

import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectReader
import org.asynchttpclient.Response
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException

private val logger = LoggerFactory.getLogger(ModernTreasuryApiException::class.java)
/**
 * Represents an error returned by the ModernTreasury API.
 */
open class ModernTreasuryApiException(
    /**
     * The HTTP Status code on the response
     */
    val httpStatus: Int,
    /**
     * The complete HTTP Response body, if it was present
     */
    val httpResponseBody: String?,
    /**
     * The error code returned from ModernTreasury, eg "parameter_invalid"
     */
    val code: String?,
    /**
     * Detailed error message.
     */
    val errorMessage: String?,
    /**
     * The parameter in error.
     */
    val parameter: String?
) : Exception() {
    override val message: String
        get() = "ModernTreasury returned an error. HTTP Status: $httpStatus. error code: '$code'. Message: '$errorMessage'. Parameter: '$parameter'"
}

/**
 * Convert an error response from ModernTreasury into a ModernTreasuryException. Throws IllegalArgumentException if the
 * response has a 2xx status code.
 */
fun Response.toModernTreasuryException(reader: ObjectReader): ModernTreasuryApiException {
    if (statusCode in 200..299) {
        throw IllegalArgumentException("Can't create a ModernTreasuryException out of a succesful response")
    }
    val errors = try {
        reader.with(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .forType(ModernTreasuryErrorBody::class.java).readValue(responseBody)
    } catch (e: Exception) {
        logger.warn("could not parse Modern Treasury error response: $responseBody. ${e.message}")
        ModernTreasuryErrorBody(null, null, null)
    }

    return if (statusCode == 409 && errors.parameter == "version") {
        LedgerAccountVersionConflictException(statusCode, responseBody, errors.code, errors.message, errors.parameter)
    } else {
        ModernTreasuryApiException(statusCode, responseBody, errors.code, errors.message, errors.parameter)
    }
}

@JsonRootName("errors")
private data class ModernTreasuryErrorBody(
    val code: String?,
    val message: String?,
    val parameter: String?
)
