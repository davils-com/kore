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
import com.davils.kore.core.limit.Limits
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.Violation
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.validation.violation.ViolationPath

internal class ViolationCollector {
    private val collected = ArrayList<Violation>()
    private var omitted: Long = 0

    fun add(violation: Violation) {
        if (collected.size < Limits.MAX_VIOLATIONS) {
            collected += violation
            return
        }
        omitted++
    }

    fun violations(): List<Violation> {
        if (omitted == 0L) return collected.immutableList()
        return (collected + omission()).immutableList()
    }

    private fun omission(): Violation = Violation(
        path = ViolationPath.ROOT,
        code = ViolationCode.TOO_MANY,
        severity = Severity.Error,
        message = "$omitted further problems were found but not collected",
    )
}
