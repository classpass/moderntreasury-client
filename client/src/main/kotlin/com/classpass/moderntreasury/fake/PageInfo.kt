package com.classpass.moderntreasury.fake

import com.classpass.moderntreasury.model.ModernTreasuryPageInfo

internal data class PageInfo(
    override val page: Int,
    override val perPage: Int,
    override val totalCount: Int
) : ModernTreasuryPageInfo
