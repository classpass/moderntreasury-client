package com.classpass.moderntreasury.exception

/**
 * HTTP 409 CONFLICT response from a request to post a transaction due to mismatched version parameters.
 */
class LedgerAccountVersionConflictException(
    httpStatus: Int,
    httpResponseBody: String?,
    code: String?,
    errorMessage: String?,
    parameter: String?
) : ModernTreasuryApiException(httpStatus, httpResponseBody, code, errorMessage, parameter)
