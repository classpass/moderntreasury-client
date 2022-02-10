package com.classpass.moderntreasury.client

/**
 * Called my the ModernTreasuryClient with response header information resulting from each request the client executes.
 */
fun interface ResponseCallback {
    fun rateLimitRemaining(rateLimitRemaining: Int)
}

val DO_NOTHING_RESPONSE_CALLBACK = ResponseCallback {
    // don't do anything
}
