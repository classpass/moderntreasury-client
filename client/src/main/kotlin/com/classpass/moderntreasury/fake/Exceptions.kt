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
package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.exception.LedgerAccountVersionConflictException
import com.classpass.moderntreasury.exception.ModernTreasuryApiException
import com.classpass.moderntreasury.exception.TransactionAlreadyPostedException

internal fun throwApiException(message: String, parameter: String? = null): Nothing = throw ModernTreasuryApiException(400, null, null, message, parameter)
internal fun throwLedgerAccountVersionConflictException(): Nothing = throw LedgerAccountVersionConflictException()
internal fun throwTransactionAlreadyPostedException(): Nothing = throw TransactionAlreadyPostedException()
