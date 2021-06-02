package com.classpass.moderntreasury

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.client.asyncModernTreasuryClient
import com.classpass.moderntreasury.config.ModernTreasuryConfig
import java.io.IOException
import java.util.Properties

/**
 * Extend this class to write a test that hits the live modern treasury api, using credentials in live-tests.properties
 */
open class ModernTreasuryLiveTest {
    private val props = Properties()
    val client: ModernTreasuryClient
    init {
        val inputStream = ClassLoader.getSystemResourceAsStream("live-tests.properties")
        try {
            props.load(inputStream)
            client =
                asyncModernTreasuryClient(ModernTreasuryConfig(props.getProperty("orgId"), props.getProperty("apiKey")))
        } catch (e: IOException) {
            throw Exception("Unable to load live-tests.properties", e)
        }
    }
}