/**
 * Copyright 2024 ClassPass
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
package com.classpass.moderntreasury.client

import com.classpass.moderntreasury.ModernTreasuryLiveTest
import org.junit.jupiter.api.Test

class LedgerCrudTest : ModernTreasuryLiveTest() {
    @Test
    fun createAndDeleteLedger() {
        val ledger = client.createLedger(
            "client_liveTests_${this::class.simpleName}_${System.currentTimeMillis()}",
            null,
            "USD",
            nextId()
        ).get()
        client.deleteLedger(ledger.id).get()
    }
}
