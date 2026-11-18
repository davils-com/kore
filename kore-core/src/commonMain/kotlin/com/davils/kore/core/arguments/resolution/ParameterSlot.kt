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

package com.davils.kore.core.arguments.resolution

import com.davils.kore.core.arguments.report.ValueOrigin
import com.davils.kore.core.arguments.report.displayedValue
import com.davils.kore.core.arguments.report.originSuffix

private const val UNASSIGNED_MESSAGE: String = "A parameter was read before its schema assigned it"

internal class ParameterSlot<V> {
    private var assigned: Assigned<V>? = null

    fun assign(value: V, origin: ValueOrigin?) {
        assigned = Assigned(value, origin)
    }

    fun value(): V = current().value

    fun describe(isSensitive: Boolean): String {
        val current = current()
        val rendered = displayedValue(current.value, isSensitive)
        val origin = current.origin ?: return rendered
        return "$rendered ${originSuffix(origin)}"
    }

    private fun current(): Assigned<V> = checkNotNull(assigned) { UNASSIGNED_MESSAGE }
}
