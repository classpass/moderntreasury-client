package com.classpass.moderntreasury

import assertk.fail
import com.classpass.moderntreasury.client.ModernTreasuryClient
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.*

interface ModernTreasuryLiveTest {
    val client: ModernTreasuryClient
}
class ModernTreasuryLiveTestImpl: ModernTreasuryLiveTest {
    private val props = Properties()
    


    init {
        val inputStream = ClassLoader.getSystemResourceAsStream("live-tests.properties")
        try {
            props.load(inputStream);
            client = AsyncModernTreasuryClient
        } catch (e: IOException) {
            throw Exception("Unable to load live-tests.properties", e)
        }
    }
}


    @Test
    fun iFail() {
        fail("live test fail")
    }
}