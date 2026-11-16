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

package com.davils.kore.core.arguments.declaration

import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreValueOrigin
import com.davils.kore.core.arguments.report.displayedValue
import com.davils.kore.core.arguments.resolution.Assignment
import com.davils.kore.core.arguments.resolution.ParameterSlot
import com.davils.kore.core.arguments.resolution.ValueReading

internal sealed interface Arity<T : Any, V> {
    val kind: ParameterKind

    fun defaultDisplay(isSensitive: Boolean): String?

    fun given(values: List<T>, origin: KoreValueOrigin, slot: ParameterSlot<V>): Assignment?

    fun absent(
        reading: ValueReading<T>,
        slot: ParameterSlot<V>,
        violations: MutableList<KoreParameterViolation>,
    ): Assignment?

    class Optional<T : Any> : Arity<T, T?> {
        override val kind: ParameterKind get() = ParameterKind.Optional

        override fun defaultDisplay(isSensitive: Boolean): String? = null

        override fun given(values: List<T>, origin: KoreValueOrigin, slot: ParameterSlot<T?>): Assignment =
            Assignment { slot.assign(values.firstOrNull(), origin) }

        override fun absent(
            reading: ValueReading<T>,
            slot: ParameterSlot<T?>,
            violations: MutableList<KoreParameterViolation>,
        ): Assignment = Assignment { slot.assign(null, null) }
    }

    class Required<T : Any> : Arity<T, T> {
        override val kind: ParameterKind get() = ParameterKind.Required

        override fun defaultDisplay(isSensitive: Boolean): String? = null

        override fun given(values: List<T>, origin: KoreValueOrigin, slot: ParameterSlot<T>): Assignment? =
            firstOf(values, origin, slot)

        override fun absent(
            reading: ValueReading<T>,
            slot: ParameterSlot<T>,
            violations: MutableList<KoreParameterViolation>,
        ): Assignment? {
            violations += KoreParameterViolation.MissingRequired(reading.info)
            return null
        }
    }

    class Defaulted<T : Any>(private val default: T) : Arity<T, T> {
        override val kind: ParameterKind get() = ParameterKind.Defaulted

        override fun defaultDisplay(isSensitive: Boolean): String = displayedValue(default, isSensitive)

        override fun given(values: List<T>, origin: KoreValueOrigin, slot: ParameterSlot<T>): Assignment? =
            firstOf(values, origin, slot)

        override fun absent(
            reading: ValueReading<T>,
            slot: ParameterSlot<T>,
            violations: MutableList<KoreParameterViolation>,
        ): Assignment? {
            if (!reading.admitsDefault(default, violations)) return null
            return Assignment { slot.assign(default, KoreValueOrigin.Default) }
        }
    }

    class Multiple<T : Any>(private val defaults: List<T>) : Arity<T, List<T>> {
        override val kind: ParameterKind get() = ParameterKind.Multiple

        override fun defaultDisplay(isSensitive: Boolean): String? {
            if (defaults.isEmpty()) return null
            return displayedValue(defaults, isSensitive)
        }

        override fun given(values: List<T>, origin: KoreValueOrigin, slot: ParameterSlot<List<T>>): Assignment =
            Assignment { slot.assign(readOnlyCopy(values), origin) }

        override fun absent(
            reading: ValueReading<T>,
            slot: ParameterSlot<List<T>>,
            violations: MutableList<KoreParameterViolation>,
        ): Assignment {
            val usable = defaults.filter { value -> reading.admitsDefault(value, violations) }
            return Assignment { slot.assign(readOnlyCopy(usable), KoreValueOrigin.Default) }
        }
    }

    class Flag(private val isOnByDefault: Boolean) : Arity<Boolean, Boolean> {
        override val kind: ParameterKind get() = ParameterKind.Flag

        override fun defaultDisplay(isSensitive: Boolean): String? = isOnByDefault.toString().takeIf { isOnByDefault }

        override fun given(values: List<Boolean>, origin: KoreValueOrigin, slot: ParameterSlot<Boolean>): Assignment? =
            firstOf(values, origin, slot)

        override fun absent(
            reading: ValueReading<Boolean>,
            slot: ParameterSlot<Boolean>,
            violations: MutableList<KoreParameterViolation>,
        ): Assignment = Assignment { slot.assign(isOnByDefault, KoreValueOrigin.Default) }
    }
}

private fun <T : Any> firstOf(values: List<T>, origin: KoreValueOrigin, slot: ParameterSlot<T>): Assignment? =
    values.firstOrNull()?.let { value -> Assignment { slot.assign(value, origin) } }
