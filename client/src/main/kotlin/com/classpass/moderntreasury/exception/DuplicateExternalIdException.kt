package com.classpass.moderntreasury.exception

private const val STATUS_CODE = 422
private val ERRORS = ModernTreasuryErrorBody(
    "parameter_invalid",
    "Another ledger transaction within the same ledger exists using the external ID you provided",
    "external_id"
)

/**
 * HTTP 422 UNPROCESSABLE ENTITY response from a request which attempts to reuse an external id.
 */
class DuplicateExternalIdException : ModernTreasuryApiException(STATUS_CODE, null, ERRORS.code, ERRORS.message, ERRORS.parameter)

internal val duplicateExternalIdExceptionMapper = ModernTreasuryApiExceptionMapper { response, errors ->
    if (response.statusCode == STATUS_CODE && errors == ERRORS) {
        DuplicateExternalIdException()
    } else null
}
