package com.classpass.moderntreasury.model.request

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

internal class ModernTreasuryTemporalQueryTest {

    @Test
    fun `Provides a useful toString for InstantQuery`() {
        val start = Instant.parse("2021-05-01T10:00:00Z")
        val finish = Instant.parse("2021-05-30T23:00:00Z")

        val instantQuery = InstantQuery().greaterThan(start).lessThanOrEqualTo(finish)
        assertEquals("InstantQuery[gt=2021-05-01T10:00:00Z,lte=2021-05-30T23:00:00Z]", instantQuery.toString())
    }

    @Test
    fun `Provides a useful toString for DateQuery`() {
        val begin = LocalDate.of(2021, 6, 1)
        val end = LocalDate.of(2021, 6, 30)

        val dateQuery = DateQuery().greaterThan(begin).lessThanOrEqualTo(end)
        assertEquals("DateQuery[gt=2021-06-01,lte=2021-06-30]", dateQuery.toString())
    }
}
