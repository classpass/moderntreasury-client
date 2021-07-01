package com.classpass.moderntreasury

import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.classpass.moderntreasury.client.asyncModernTreasuryClient
import com.classpass.moderntreasury.config.ModernTreasuryConfig
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.Properties

/**
 * Extend this class to write a test that hits the live modern treasury api, using credentials in live-tests.properties
 */
open class ModernTreasuryLiveTest {
    companion object {
        fun nextId() = Math.random().toString()
    }
    private val props = Properties()
    val client: ModernTreasuryClient
    init {
        val inputStream = ClassLoader.getSystemResourceAsStream("live-tests.properties")
        try {
            props.load(inputStream)
            if(!props.getProperty("apiKey").contains("test")) {
                throw IllegalArgumentException("Live tests must use the test API Key!")
            }
            client =
                asyncModernTreasuryClient(ModernTreasuryConfig(props.getProperty("orgId"), props.getProperty("apiKey")))
        } catch (e: IOException) {
            throw Exception("Unable to load live-tests.properties", e)
        }
    }
}
