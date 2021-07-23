package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.exception.LedgerAccountVersionConflictException
import com.classpass.moderntreasury.exception.ModernTreasuryApiException
import com.classpass.moderntreasury.exception.TransactionAlreadyPostedException

internal fun throwApiException(message: String, parameter: String? = null): Nothing = throw ModernTreasuryApiException(400, null, null, message, parameter)
internal fun throwLedgerAccountVersionConflictException(): Nothing = throw LedgerAccountVersionConflictException()
internal fun throwTransactionAlreadyPostedException(): Nothing = throw TransactionAlreadyPostedException()
