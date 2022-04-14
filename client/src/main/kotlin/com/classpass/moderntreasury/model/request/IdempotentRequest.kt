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
package com.classpass.moderntreasury.model.request

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * The Modern Treasury API supports idempotent requests to help prevent you from accidentally issuing the same API call
 * twice. This feature can be helpful if you are instructing us to move money, create an entity, or make changes to an
 * existing resource. For example, if you are creating a Payment Order and the request fails due to a network issue, you
 * can retry the request with the same idempotency key to guarantee the payment order was only created once.
 *
 * When a successful request is made with an idempotency key, ModernTreasury will save the result of that request for 24
 * hours. If you issue a new request within 24 hours using the same credentials and idempotency key, ModernTreasury will
 * echo the original response (status code and body) back to you.
 *
 * A few things to note:
 * - Results are only saved if the request successfully executed.
 * - If there was a connection interruption or another error that prevented us from completing the original request,
 *   ModernTreasury will execute the following request and cache its results.
 * - Idempotency keys are independent of the route. For example if you use a key to create a payment order and then use
 *   the same key to create a counterparty within 24 hours, you will received the cached result of the first payment order request.
 *
 * MT Docs reference: https://docs.moderntreasury.com/reference#idempotent-requests
 */
interface IdempotentRequest {
    /**
     * Unique idempotency key for this request. Requests with the same idempotency key can be retried without fear of
     * issuing the same API call multiple times.
     */
    @get:JsonIgnore
    val idempotencyKey: String
}
