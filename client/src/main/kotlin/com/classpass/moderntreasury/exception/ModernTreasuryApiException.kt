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

import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectReader
import org.asynchttpclient.Response
import org.slf4j.LoggerFactory

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

    val exceptionMappers = listOf(
        ledgerAccountVersionConflictExceptionMapper,
        transactionAlreadyPostedExceptionMapper,
        duplicateExternalIdExceptionMapper
    )
    return exceptionMappers.mapNotNull { it.map(this, errors) }.firstOrNull() ?: ModernTreasuryApiException(
        statusCode,
        responseBody,
        errors.code,
        errors.message,
        errors.parameter
    )
}

@JsonRootName("errors")
internal data class ModernTreasuryErrorBody(
    val code: String?,
    val message: String?,
    val parameter: String?
)

/**
 * This interface is used to convert an error Response into one of our subclasses of ModernTreasuryApiException. Each
 * defined subclass of ModernTreasuryApiException should declare a mapper that implements this interface and include it
 * in Response.toModernTreasuryException defined above.
 */
internal fun interface ModernTreasuryApiExceptionMapper<T : ModernTreasuryApiException> {
    /**
     * Map an error response from Modern Treasury to a ModernTreasuryApiException of type T. Returns null if the
     * response is not an error of type T
     */
    fun map(response: Response, errors: ModernTreasuryErrorBody): T?
}
