package com.classpass.moderntreasury.exception

/**
 * Thrown if the rate limiter cannot grant a permit within the configured timeout. If this exception is thrown, then no
 * request was sent to ModernTreasury.
 */
class RateLimitTimeoutException internal constructor(rateLimitTimeoutMs: Long) :
    Exception("Unable to acquire a permit from the rate limiter within $rateLimitTimeoutMs milliseconds. Request not sent")
