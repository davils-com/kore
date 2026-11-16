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

internal const val INTERVAL_SHAPE_REASON: String = "has an interval that is not written like [1.2, 2.0)"

private const val LIST_SEPARATOR: Char = ','
private const val INCLUSIVE_CLOSING: Char = ']'
private const val EXCLUSIVE_CLOSING: Char = ')'

internal fun readIntervals(text: String, collector: RangeCollector) {
    var position = 0
    while (position < text.length && !collector.isRefused) {
        position = readIntervalAt(text, position, collector)
    }
}

private fun readIntervalAt(text: String, position: Int, collector: RangeCollector): Int {
    val closing = closingAt(text, position)
    val isShaped = text[position] in INTERVAL_OPENINGS && closing < text.length
    if (!isShaped) return refusedShape(text, collector)
    collector.count(1)
    collector.collect(readInterval(text.substring(startIndex = position, endIndex = closing + 1)))
    return nextIntervalAt(text, closing + 1, collector)
}

private fun closingAt(text: String, from: Int): Int {
    var index = from
    while (index < text.length && text[index] != INCLUSIVE_CLOSING && text[index] != EXCLUSIVE_CLOSING) {
        index++
    }
    return index
}

private fun nextIntervalAt(text: String, from: Int, collector: RangeCollector): Int {
    val separator = skipRangeSpace(text, from)
    if (separator == text.length) return separator
    val next = skipRangeSpace(text, separator + 1)
    val isSeparated = text[separator] == LIST_SEPARATOR && next < text.length
    if (!isSeparated) collector.refuse(INTERVAL_SHAPE_REASON)
    return next
}

private fun refusedShape(text: String, collector: RangeCollector): Int {
    collector.refuse(INTERVAL_SHAPE_REASON)
    return text.length
}
