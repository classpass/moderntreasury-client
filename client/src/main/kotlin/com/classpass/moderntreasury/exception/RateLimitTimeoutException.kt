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

/**
 * Thrown if the rate limiter cannot grant a permit within the configured timeout. If this exception is thrown, then no
 * request was sent to ModernTreasury.
 */
class RateLimitTimeoutException internal constructor(rateLimitTimeoutMs: Long) :
    Exception("Unable to acquire a permit from the rate limiter within $rateLimitTimeoutMs milliseconds. Request not sent")
