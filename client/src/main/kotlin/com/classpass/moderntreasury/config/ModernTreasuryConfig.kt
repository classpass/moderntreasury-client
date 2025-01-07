/**
 * Copyright 2025 ClassPass
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
package com.classpass.moderntreasury.config

class ModernTreasuryConfig(
    val organizationId: String,
    val apiKey: String,
    val baseUrl: String = DEFAULT_BASE_URL,
    val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT,
    val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT,
    val requestTimeoutMs: Int = DEFAULT_REQUEST_TIMEOUT,
    val httpClientCodecMaxHeaderSize: Int = DEFAULT_HTTP_CLIENT_CODEC_MAX_HEADER_SIZE,
    /**
     * Sets a limit on how long to wait for the rate limiter before submitting a request. If a request cannot pass the
     * rate limiter before rateLimitTimeoutMs will occur, the request will fail with a RateLimitTimeoutException. This
     * option has no effect if maxRequestsPerSecond is null.
     */
    val rateLimitTimeoutMs: Long = DEFAULT_RATE_LIMIT_TIMEOUT,
    /**
     * Limits the number of requests made per second to modern treasury. Requests made after the limit has been reached
     * will be delayed. null for no rate limiting. If set, must be positive.
     */
    maxRequestsPerSecond: Int? = null
) {
    val rateLimit = maxRequestsPerSecond?.toDouble() ?: Double.MAX_VALUE
}
private const val DEFAULT_BASE_URL = "https://app.moderntreasury.com/api"
private const val DEFAULT_CONNECT_TIMEOUT = 1_000
private const val DEFAULT_READ_TIMEOUT = 3_000
private const val DEFAULT_REQUEST_TIMEOUT = 3_000
private const val DEFAULT_RATE_LIMIT_TIMEOUT = 10_000L
private const val DEFAULT_HTTP_CLIENT_CODEC_MAX_HEADER_SIZE = 16384
