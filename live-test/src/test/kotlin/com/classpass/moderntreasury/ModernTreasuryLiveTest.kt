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
            if (!props.getProperty("apiKey").contains("test")) {
                throw IllegalArgumentException("Live tests must use the test API Key!")
            }
            client =
                asyncModernTreasuryClient(
                    ModernTreasuryConfig(props.getProperty("orgId"), props.getProperty("apiKey"))
                ) {
                    println("In ModernTreasuryLiveTest, rate limit remaining was $it")
                }
        } catch (e: IOException) {
            throw Exception("Unable to load live-tests.properties", e)
        }
    }
}
