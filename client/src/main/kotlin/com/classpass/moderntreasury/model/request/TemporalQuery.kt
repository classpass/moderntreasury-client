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
    TemporalQuery<LocalDate>(queryParts) {
    /**
     * Create an empty DateQuery
     */
    constructor() : this(emptyList())
    override fun plus(preposition: DatePreposition, temporal: LocalDate): TemporalQuery<LocalDate> =
        DateQuery(this.queryParts.plus(TemporalQueryPart(preposition, temporal)))
}
/**
 * A class for constructing datetime-related query parameters for modern treasury queries. Usage example:
 * DateTimeQuery().lessThan(endTime).greaterThanOrEqualTo(startTime)
 * or
 * DateTimeQuery().equalTo(timestamp)
 */
class DateTimeQuery internal constructor(queryParts: List<TemporalQueryPart<ZonedDateTime>>) :
    TemporalQuery<ZonedDateTime>(queryParts) {
    /**
     * Create an empty DateTimeQuery
     */
    constructor() : this(emptyList())
    override fun plus(preposition: DatePreposition, temporal: ZonedDateTime): TemporalQuery<ZonedDateTime> =
        DateTimeQuery(this.queryParts.plus(TemporalQueryPart(preposition, temporal)))
}

sealed class TemporalQuery<T : Temporal> constructor(
    internal val queryParts: List<TemporalQueryPart<T>>
) {

    fun lessThan(temporal: T): TemporalQuery<T> = this.plus(DatePreposition.LESS_THAN, temporal)
    fun lessThanOrEqualTo(temporal: T): TemporalQuery<T> = this.plus(DatePreposition.LESS_THAN_OR_EQUAL_TO, temporal)
    fun greaterThan(temporal: T): TemporalQuery<T> = this.plus(DatePreposition.GREATER_THAN, temporal)
    fun greaterThanOrEqualTo(temporal: T): TemporalQuery<T> = this.plus(DatePreposition.GREATER_THAN_OR_EQUAL_TO, temporal)
    fun equalTo(temporal: T): TemporalQuery<T> = this.plus(DatePreposition.EQUAL_TO, temporal)

    fun toQueryParams(fieldName: String) = queryParts.map {
        "$fieldName%5B${it.preposition.key}%5D" to listOf(it.temporal.toString())
    }.toMap()

    internal abstract fun plus(preposition: DatePreposition, temporal: T): TemporalQuery<T>
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
