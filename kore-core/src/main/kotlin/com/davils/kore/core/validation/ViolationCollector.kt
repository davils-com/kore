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

package com.davils.kore.core.validation

import com.davils.kore.core.immutableList
import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.validation.violation.KoreSeverity
import com.davils.kore.core.validation.violation.KoreViolation
import com.davils.kore.core.validation.violation.KoreViolationCode
import com.davils.kore.core.validation.violation.KoreViolationPath

internal class ViolationCollector {
    private val collected = ArrayList<KoreViolation>()
    private var omitted: Long = 0

    fun add(violation: KoreViolation) {
        if (collected.size < KoreLimits.MAX_VIOLATIONS) {
            collected += violation
            return
        }
        omitted++
    }

    fun violations(): List<KoreViolation> {
        if (omitted == 0L) return collected.immutableList()
        return (collected + omission()).immutableList()
    }

    private fun omission(): KoreViolation = KoreViolation(
        path = KoreViolationPath.ROOT,
        code = KoreViolationCode.TOO_MANY,
        severity = KoreSeverity.Error,
        message = "$omitted further problems were found but not collected",
    )
}
