package com.classpass.moderntreasury.config

import com.classpass.moderntreasury.client.AsyncModernTreasuryClient
import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import org.asynchttpclient.Dsl
import org.asynchttpclient.Realm

class ModernTreasuryModule : AbstractModule() {
    override fun configure() {}

    @Provides
    @Singleton
    fun providesInventoryServiceClient(
        config: ModernTreasuryConfig
    ): ModernTreasuryClient {
        val clientConfig = Dsl.config()
            .setRealm(Dsl.basicAuthRealm(config.organizationId, config.apiKey))
            .setConnectTimeout(config.connectTimeoutMs)
            .setReadTimeout(config.readTimeoutMs)
            .setRequestTimeout(config.requestTimeoutMs)
            .build()
        val asyncHttpClient = Dsl.asyncHttpClient(clientConfig)
        return AsyncModernTreasuryClient(config.baseUrl, asyncHttpClient)
    }
}