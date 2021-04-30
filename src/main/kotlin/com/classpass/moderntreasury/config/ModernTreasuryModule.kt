package com.classpass.moderntreasury.config

import com.classpass.moderntreasury.client.AsyncModernTreasuryClient
import com.classpass.moderntreasury.client.ModernTreasuryClient
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton

class ModernTreasuryModule(private val config: ModernTreasuryConfig) : AbstractModule() {

    @Provides
    @Singleton
    fun providesModernTreasuryClient(): ModernTreasuryClient = AsyncModernTreasuryClient.create(config)
}
