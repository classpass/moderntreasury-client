package com.classpass.moderntreasury.model.request

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.Temporal

/**
 * A class for constructing date-related query parameters for modern treasury queries. Usage example:
 * DateQuery().lessThan(endDate).greaterThanOrEqualTo(startDate)
 * or
 * DateQuery().equalTo(date)
 */
class DateQuery internal constructor(queryParts: List<TemporalQueryPart<LocalDate>>) :
    ModernTreasuryTemporalQuery<LocalDate>(queryParts) {
    /**
     * Create an empty DateQuery
     */
    constructor() : this(emptyList())

    override fun plus(preposition: DatePreposition, temporal: LocalDate): DateQuery =
        DateQuery(this.queryParts.plus(TemporalQueryPart(preposition, temporal)))

    override fun lessThan(temporal: LocalDate) = super.lessThan(temporal) as DateQuery
    override fun lessThanOrEqualTo(temporal: LocalDate) = super.lessThanOrEqualTo(temporal) as DateQuery
    override fun greaterThan(temporal: LocalDate) = super.greaterThan(temporal) as DateQuery
    override fun greaterThanOrEqualTo(temporal: LocalDate) = super.greaterThanOrEqualTo(temporal) as DateQuery
    override fun equalTo(temporal: LocalDate) = super.equalTo(temporal) as DateQuery
}

/**
 * A class for constructing datetime-related query parameters for modern treasury queries. Usage example:
 * DateTimeQuery().lessThan(endTime).greaterThanOrEqualTo(startTime)
 * or
 * DateTimeQuery().equalTo(timestamp)
 */
class DateTimeQuery internal constructor(queryParts: List<TemporalQueryPart<ZonedDateTime>>) :
    ModernTreasuryTemporalQuery<ZonedDateTime>(queryParts) {
    /**
     * Create an empty DateTimeQuery
     */
    constructor() : this(emptyList())

    override fun plus(preposition: DatePreposition, temporal: ZonedDateTime): DateTimeQuery =
        DateTimeQuery(this.queryParts.plus(TemporalQueryPart(preposition, temporal)))

    override fun lessThan(temporal: ZonedDateTime) = super.lessThan(temporal) as DateTimeQuery
    override fun lessThanOrEqualTo(temporal: ZonedDateTime) = super.lessThanOrEqualTo(temporal) as DateTimeQuery
    override fun greaterThan(temporal: ZonedDateTime) = super.greaterThan(temporal) as DateTimeQuery
    override fun greaterThanOrEqualTo(temporal: ZonedDateTime) = super.greaterThanOrEqualTo(temporal) as DateTimeQuery
    override fun equalTo(temporal: ZonedDateTime) = super.equalTo(temporal) as DateTimeQuery
}

sealed class ModernTreasuryTemporalQuery<T : Temporal> constructor(
    internal val queryParts: List<TemporalQueryPart<T>>
) {

    open fun lessThan(temporal: T) = this.plus(DatePreposition.LESS_THAN, temporal)
    open fun lessThanOrEqualTo(temporal: T) = this.plus(DatePreposition.LESS_THAN_OR_EQUAL_TO, temporal)
    open fun greaterThan(temporal: T) = this.plus(DatePreposition.GREATER_THAN, temporal)
    open fun greaterThanOrEqualTo(temporal: T) = this.plus(DatePreposition.GREATER_THAN_OR_EQUAL_TO, temporal)
    open fun equalTo(temporal: T) = this.plus(DatePreposition.EQUAL_TO, temporal)

    fun toQueryParams(fieldName: String) = queryParts.map {
        "$fieldName[${it.preposition.key}]" to listOf(it.temporal.toString())
    }.toMap()

    internal abstract fun plus(preposition: DatePreposition, temporal: T): ModernTreasuryTemporalQuery<T>
}

internal enum class DatePreposition(val key: String) {
    GREATER_THAN("gt"),
    GREATER_THAN_OR_EQUAL_TO("gte"),
    LESS_THAN("lt"),
    LESS_THAN_OR_EQUAL_TO("lte"),
    EQUAL_TO("eq")
}

internal class TemporalQueryPart<T : Temporal>(
    val preposition: DatePreposition,
    val temporal: T
)
