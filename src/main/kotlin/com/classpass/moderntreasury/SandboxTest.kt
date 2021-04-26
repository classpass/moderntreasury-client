@file:JvmName("SandboxTest")

package com.classpass.moderntreasury

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.config.ModernTreasuryConfig
import com.classpass.moderntreasury.config.ModernTreasuryModule
import com.google.inject.Guice
import kotlin.system.exitProcess

/**
 * This main method will ping the actual modern treasury api and then exit. Used as a sanity check to make sure
 * our api keys are working. Jenkins can't communicate with external services during testing so you can run this manually
 * like this:
 * ./gradlew sandbox-test --args="$ORGANIZAION_ID $API_SECRET $BASE_URL"
 */
fun main(args: Array<String>) {
    val config = ModernTreasuryConfig(args[0], args[1], args[2])
    val client = Guice.createInjector(ModernTreasuryModule(config)).getInstance(ModernTreasuryClient::class.java)
    try {
        client.ping().get()
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }
    exitProcess(0)
}