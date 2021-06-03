package com.classpass.moderntreasury.exception

private const val STATUS_CODE = 422
private val ERRORS = ModernTreasuryErrorBody(
    "parameter_invalid",
    "The ledger transaction write failed because at least one of the provided ledger account versions is incorrect",
    "ledger_entries"
)

/**
 * HTTP 422 UNPROCESSABLE response from a request to post a transaction due to mismatched version parameters.
 */
class LedgerAccountVersionConflictException :
    ModernTreasuryApiException(STATUS_CODE, null, ERRORS.code, ERRORS.message, ERRORS.parameter)

internal val ledgerAccountVersionConflictExceptionMapper = ModernTreasuryApiExceptionMapper { response, errors ->
    if (response.statusCode == STATUS_CODE && errors == ERRORS) {
        LedgerAccountVersionConflictException()
    } else null
}
