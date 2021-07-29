package com.classpass.moderntreasury.model.request

import java.time.LocalDate

/**
 * A class for constructing date-related query parameters for modern treasury queries. Usage example:
 * DateQuery().lessThan(endDate).greaterThanOrEqualTo(startDate)
 * or
 * DateQuery().equalTo(localDate)
 */
class DateQuery private constructor(
    private val queryParts: List<DateQueryPart>
) {
    /**
     * Create an empty DateQuery
     */
    constructor() : this(emptyList())

    fun lessThan(date: LocalDate): DateQuery = this.plus(DatePreposition.LESS_THAN, date)
    fun lessThanOrEqualTo(date: LocalDate): DateQuery = this.plus(DatePreposition.LESS_THAN_OR_EQUAL_TO, date)
    fun greaterThan(date: LocalDate): DateQuery = this.plus(DatePreposition.GREATER_THAN, date)
    fun greaterThanOrEqualTo(date: LocalDate): DateQuery = this.plus(DatePreposition.GREATER_THAN_OR_EQUAL_TO, date)
    fun equalTo(date: LocalDate): DateQuery = this.plus(DatePreposition.EQUAL_TO, date)

    private fun plus(preposition: DatePreposition, date: LocalDate) = DateQuery(queryParts.plus(DateQueryPart(preposition, date)))
}

private enum class DatePreposition {
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL_TO,
    LESS_THAN,
    LESS_THAN_OR_EQUAL_TO,
    EQUAL_TO
}

private class DateQueryPart(
    val preposition: DatePreposition,
    val date: LocalDate
)
