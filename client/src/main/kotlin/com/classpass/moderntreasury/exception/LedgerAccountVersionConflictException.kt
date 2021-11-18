/**
 * Copyright 2021 ClassPass
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
