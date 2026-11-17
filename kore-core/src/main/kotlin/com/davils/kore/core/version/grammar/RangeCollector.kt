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

import com.davils.kore.core.limit.Limits
import com.davils.kore.core.version.interval.VersionInterval

private const val TOO_MANY_COMPARATORS_REASON: String =
    "has more than ${Limits.MAX_RANGE_COMPARATORS} comparators"

internal class RangeCollector {
    private val collected: MutableList<VersionInterval> = mutableListOf()
    private var comparators: Int = 0
    private var refusal: String? = null

    internal val isRefused: Boolean get() = refusal != null

    internal fun count(terms: Int) {
        comparators += terms
        if (comparators > Limits.MAX_RANGE_COMPARATORS) refuse(TOO_MANY_COMPARATORS_REASON)
    }

    internal fun refuse(reason: String) {
        refusal = refusal ?: reason
    }

    internal fun collect(reading: RangeReading) {
        if (isRefused) return
        when (reading) {
            is RangeReading.Read -> collected += reading.intervals
            is RangeReading.Refused -> refuse(reading.reason)
        }
    }

    internal fun reading(): RangeReading {
        val reason = refusal ?: return RangeReading.Read(collected.toList())
        return RangeReading.Refused(reason)
    }
}
