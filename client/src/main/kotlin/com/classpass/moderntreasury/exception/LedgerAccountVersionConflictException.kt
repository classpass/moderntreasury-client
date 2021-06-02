package com.classpass.moderntreasury.exception

private const val STATUS_CODE = 409
private const val MESSAGE =
    "The ledger transaction write failed because at least one of the provided ledger account versions is incorrect"

/**
 * HTTP 409 CONFLICT response from a request to post a transaction due to mismatched version parameters.
 */
class LedgerAccountVersionConflictException(
    httpResponseBody: String?,
    code: String?,
    parameter: String?
) : ModernTreasuryApiException(409, httpResponseBody, code, MESSAGE, parameter)

internal val ledgerAccountVersionConflictExceptionMapper = ModernTreasuryApiExceptionMapper { response, errors ->
    if (response.statusCode == STATUS_CODE && errors.message == MESSAGE) {
        LedgerAccountVersionConflictException(response.responseBody, errors.code, errors.parameter)
    } else null
}
