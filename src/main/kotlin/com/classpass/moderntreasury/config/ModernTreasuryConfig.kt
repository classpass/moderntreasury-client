package com.classpass.moderntreasury.config

class ModernTreasuryConfig(
    val organizationId: String,
    val apiKey: String,
    val baseUrl: String = DEFAULT_BASE_URL,
    val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT,
    val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT,
    val requestTimeoutMs: Int = DEFAULT_REQUEST_TIMEOUT
)
private const val DEFAULT_BASE_URL = "https://app.moderntreasury.com/api"
private const val DEFAULT_CONNECT_TIMEOUT = 1000
private const val DEFAULT_READ_TIMEOUT = 3000
private const val DEFAULT_REQUEST_TIMEOUT = 3000
