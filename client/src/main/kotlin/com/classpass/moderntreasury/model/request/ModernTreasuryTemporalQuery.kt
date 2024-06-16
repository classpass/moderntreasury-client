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
package com.classpass.moderntreasury.model.request

import java.time.Instant
import java.time.LocalDate
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
 * A class for constructing timestamp-related query parameters for modern treasury queries. Usage example:
 * InstantQuery().lessThan(endTime).greaterThanOrEqualTo(startTime)
 * or
 * InstantQuery().equalTo(timestamp)
 */
class InstantQuery internal constructor(queryParts: List<TemporalQueryPart<Instant>>) :
    ModernTreasuryTemporalQuery<Instant>(queryParts) {
    /**
     * Create an empty DateTimeQuery
     */
    constructor() : this(emptyList())

    override fun plus(preposition: DatePreposition, temporal: Instant): InstantQuery =
        InstantQuery(this.queryParts.plus(TemporalQueryPart(preposition, temporal)))

    override fun lessThan(temporal: Instant) = super.lessThan(temporal) as InstantQuery
    override fun lessThanOrEqualTo(temporal: Instant) = super.lessThanOrEqualTo(temporal) as InstantQuery
    override fun greaterThan(temporal: Instant) = super.greaterThan(temporal) as InstantQuery
    override fun greaterThanOrEqualTo(temporal: Instant) = super.greaterThanOrEqualTo(temporal) as InstantQuery
    override fun equalTo(temporal: Instant) = super.equalTo(temporal) as InstantQuery
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

    override fun toString(): String {
        return this::class.java.simpleName + queryParts.toString().filterNot { it.isWhitespace() }
    }
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
) {
    override fun toString(): String {
        return "${preposition.key}=$temporal"
    }
}
