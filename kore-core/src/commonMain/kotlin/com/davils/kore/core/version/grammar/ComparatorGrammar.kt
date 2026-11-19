/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kore.core.version.grammar

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.version.interval.EVERYTHING
import com.davils.kore.core.version.interval.intersection

private const val DANGLING_OPERATOR_REASON: String = "has an operator with no version after it"
private const val HYPHEN_REASON: String = "has a hyphen range that is not written like 1.2 - 1.4"
private const val COMPARATOR_REASON: String = "has a comparator that is not a version: it"
private const val HYPHEN: String = "-"
private const val HYPHEN_TOKENS: Int = 3

private val operatorSymbols: Set<String> = Operator.entries.map(Operator::symbol).toSet()

internal fun readComparatorSet(text: String, collector: RangeCollector) {
    val tokens = rangeTokens(text)
    if (HYPHEN in tokens) return readHyphen(tokens, collector)
    readComparators(joinedOperators(tokens), collector)
}

private fun readHyphen(tokens: List<String>, collector: RangeCollector) {
    collector.count(1)
    collector.collect(hyphenReading(tokens))
}

private fun readComparators(comparators: List<String>?, collector: RangeCollector) {
    if (comparators == null) return collector.refuse(DANGLING_OPERATOR_REASON)
    collector.count(comparators.size)
    if (collector.isRefused) return
    val readings = comparators.map(::readComparator)
    collector.collect(matchingSomething(combined(readings, listOf(EVERYTHING), ::intersection)))
}

private fun matchingSomething(reading: RangeReading): RangeReading {
    val isNothingMatched = reading is Outcome.Success && reading.value.isEmpty()
    if (isNothingMatched) return Outcome.Failure(MATCHES_NOTHING_REASON)
    return reading
}

private fun joinedOperators(tokens: List<String>): List<String>? {
    val joined = mutableListOf<String>()
    var pending = ""
    var isDangling = false
    for (token in tokens) {
        val isOperatorAfterLoneOperator = pending.isNotEmpty() && startsWithOperator(token)
        isDangling = isDangling || isOperatorAfterLoneOperator
        pending += token
        if (token !in operatorSymbols) {
            joined += pending
            pending = ""
        }
    }
    val isComplete = !isDangling && pending.isEmpty()
    return joined.takeIf { isComplete }
}

private fun startsWithOperator(token: String): Boolean = operatorSymbols.any { symbol -> token.startsWith(symbol) }

private fun readComparator(text: String): RangeReading {
    val operator = Operator.entries.firstOrNull { candidate -> text.startsWith(candidate.symbol) }
    val operand = text.removePrefix(operator?.symbol.orEmpty())
    return when (val reading = readPartial(operand)) {
        is Outcome.Success -> Outcome.Success(
            listOfNotNull(intervalFor(operator ?: Operator.Equal, reading.value)),
        )

        is Outcome.Failure -> comparatorRefusal(reading.failure)
    }
}

private fun hyphenReading(tokens: List<String>): RangeReading {
    val isShaped = tokens.size == HYPHEN_TOKENS && tokens[1] == HYPHEN
    if (!isShaped) return Outcome.Failure(HYPHEN_REASON)
    return hyphenBetween(from = readPartial(tokens[0]), to = readPartial(tokens[2]))
}

private fun hyphenBetween(from: PartialReading, to: PartialReading): RangeReading = when (from) {
    is Outcome.Failure -> comparatorRefusal(from.failure)
    is Outcome.Success -> hyphenUpTo(from.value, to)
}

private fun hyphenUpTo(from: Partial, to: PartialReading): RangeReading = when (to) {
    is Outcome.Failure -> comparatorRefusal(to.failure)
    is Outcome.Success -> readingOf(hyphenInterval(from, to.value), MATCHES_NOTHING_REASON)
}

private fun comparatorRefusal(reason: String): RangeReading = Outcome.Failure("$COMPARATOR_REASON $reason")
