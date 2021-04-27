package com.classpass.moderntreasury.config

import com.classpass.moderntreasury.client.AsyncModernTreasuryClient
import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import org.asynchttpclient.Dsl

class ModernTreasuryModule(private val config: ModernTreasuryConfig) : AbstractModule() {

    @Provides
    @Singleton
    fun providesInventoryServiceClient(): ModernTreasuryClient {
        val clientConfig = Dsl.config()
            .setRealm(Dsl.basicAuthRealm(config.organizationId, config.apiKey).setUsePreemptiveAuth(true))
            .setConnectTimeout(config.connectTimeoutMs)
            .setReadTimeout(config.readTimeoutMs)
            .setRequestTimeout(config.requestTimeoutMs)
            .build()
        val asyncHttpClient = Dsl.asyncHttpClient(clientConfig)
        return AsyncModernTreasuryClient(config.baseUrl, asyncHttpClient)
    }
}
