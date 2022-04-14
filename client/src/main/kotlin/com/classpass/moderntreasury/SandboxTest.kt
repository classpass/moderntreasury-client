/**
 * Copyright 2022 ClassPass
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
@file:JvmName("SandboxTest")

package com.classpass.moderntreasury

import com.classpass.moderntreasury.client.AsyncModernTreasuryClient
import com.classpass.moderntreasury.client.DO_NOTHING_RESPONSE_CALLBACK
import com.classpass.moderntreasury.config.ModernTreasuryConfig

/**
 * This main method will ping the actual modern treasury api and then exit. Used as a sanity check to make sure
 * your api keys are working. You can run this function manually like this:
 * ./gradlew sandbox-test --args="$ORGANIZAION_ID $API_SECRET $BASE_URL"
 */
fun main(args: Array<String>) {
    val config = ModernTreasuryConfig(args[0], args[1], args[2])
    AsyncModernTreasuryClient.create(config, DO_NOTHING_RESPONSE_CALLBACK).use { client ->
        client.ping().get()
    }
}
