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
     * rate limiter before rateLimitTimeoutMs will occur, the request will fail with a RateLimitTimeoutException.
     */
    val rateLimitTimeoutMs: Long = DEFAULT_RATE_LIMIT_TIMEOUT,
    /**
     * Limits the number of requests made per second to modern treasury. Requests made after the limit has been reached
     * will be delayed. 0 for unlimited.
     */
    maxRequestsPerSecond: Int = 0
) {
    val rateLimit = if (maxRequestsPerSecond < 1) {
        Double.MAX_VALUE
    } else {
        maxRequestsPerSecond.toDouble()
    }
}
private const val DEFAULT_BASE_URL = "https://app.moderntreasury.com/api"
private const val DEFAULT_CONNECT_TIMEOUT = 1000
private const val DEFAULT_READ_TIMEOUT = 3000
private const val DEFAULT_REQUEST_TIMEOUT = 3000
private const val DEFAULT_RATE_LIMIT_TIMEOUT = 10000L // ten seconds
