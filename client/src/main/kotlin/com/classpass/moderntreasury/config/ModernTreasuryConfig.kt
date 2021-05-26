package com.classpass.moderntreasury.config

class ModernTreasuryConfig(
    val organizationId: String,
    val apiKey: String,
    val baseUrl: String = DEFAULT_BASE_URL,
    val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT,
    val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT,
    val requestTimeoutMs: Int = DEFAULT_REQUEST_TIMEOUT,
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
