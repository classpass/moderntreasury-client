@file:JvmName("SandboxTest")

package com.classpass.moderntreasury

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.classpass.moderntreasury.config.ModernTreasuryModule
import com.google.inject.Guice

/**
 * This main method will ping the actual modern treasury api and then exit. Used as a sanity check to make sure
 * your api keys are working. You can run this function manually like this:
 * ./gradlew sandbox-test --args="$ORGANIZAION_ID $API_SECRET $BASE_URL"
 */
fun main(args: Array<String>) {
    val config = ModernTreasuryConfig(args[0], args[1], args[2])
    Guice.createInjector(ModernTreasuryModule(config)).getInstance(ModernTreasuryClient::class.java).use { client ->
        client.ping().get()
    }
}
