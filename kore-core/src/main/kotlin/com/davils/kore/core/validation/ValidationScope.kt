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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.limit.Limits
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.Violation
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.validation.violation.ViolationPath
import kotlin.reflect.KProperty

/**
 * Collects the violations of one validation, each at the path where it was found.
 *
 * Kore creates a scope for every validation, for example in [validate], and hands it to [Validatable.validate];
 * its constructors are internal, so Kotlin code outside Kore cannot create one. A scope and the nested scopes of
 * [within] and [each] share one list of violations and keep the order in which problems were reported. A scope is not
 * thread-safe: it is confined to the thread that runs the validation and must not be kept or used once
 * [Validatable.validate] has returned.
 *
 * A validation collects at most [Limits.MAX_VIOLATIONS] violations and only counts the rest, which its report
 * names in one final [ViolationCode.TOO_MANY] error, an error even when every violation it counts is a warning,
 * so a value with that many warnings is invalid. It also stops descending below [Limits.MAX_PATH_DEPTH]
 * levels with a [ViolationCode.TOO_DEEP] error, so a hostile value can exhaust neither the memory nor the stack of
 * the process. The scope does not limit how many elements [each] visits; a rule that validates a collection from
 * input bounds its size first.
 *
 * Messages, names and keys are kept exactly as given and escaped wherever a violation is shown. They are never masked,
 * so a message must not quote a secret. Entries of a map are visited in the iteration order of that map; a validation
 * that must report in a stable order hands over a sorted map.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ValidationScope internal constructor(
    private val prefix: ViolationPath,
    private val defaultCode: ViolationCode,
    private val collector: ViolationCollector,
) {
    internal constructor(prefix: ViolationPath, defaultCode: ViolationCode) :
        this(prefix, defaultCode, ViolationCollector())

    /**
     * Reports an error at the current path, which makes the validated value invalid.
     *
     * @param message what is wrong, such as `must not be blank`; text from the validated input may be part of it and
     * is escaped when shown, but a secret must never be.
     * @param code the stable name of the kind of problem; [ViolationCode.INVALID] unless given.
     * @since 1.0.0
     */
    public fun fail(message: String, code: ViolationCode = defaultCode) {
        record(path = prefix, code = code, severity = Severity.Error, message = message)
    }

    /**
     * Reports an error at [property] below the current path, which makes the validated value invalid.
     *
     * @param property the property that is wrong; only its name is used.
     * @param message what is wrong, such as `must not be blank`; text from the validated input may be part of it and
     * is escaped when shown, but a secret must never be.
     * @param code the stable name of the kind of problem; [ViolationCode.INVALID] unless given.
     * @since 1.0.0
     */
    public fun fail(property: KProperty<*>, message: String, code: ViolationCode = defaultCode) {
        record(path = prefix.name(property), code = code, severity = Severity.Error, message = message)
    }

    /**
     * Reports a warning at the current path, which leaves the validated value valid.
     *
     * @param message what is questionable; text from the validated input may be part of it and is escaped when shown,
     * but a secret must never be.
     * @param code the stable name of the kind of problem; [ViolationCode.INVALID] unless given.
     * @since 1.0.0
     */
    public fun warn(message: String, code: ViolationCode = defaultCode) {
        record(path = prefix, code = code, severity = Severity.Warning, message = message)
    }

    /**
     * Reports a warning at [property] below the current path, which leaves the validated value valid.
     *
     * @param property the property that is questionable; only its name is used.
     * @param message what is questionable; text from the validated input may be part of it and is escaped when shown,
     * but a secret must never be.
     * @param code the stable name of the kind of problem; [ViolationCode.INVALID] unless given.
     * @since 1.0.0
     */
    public fun warn(property: KProperty<*>, message: String, code: ViolationCode = defaultCode) {
        record(path = prefix.name(property), code = code, severity = Severity.Warning, message = message)
    }

    /**
     * Validates a nested value whose violations are reported below [property].
     *
     * The [block] runs at once, on the calling thread, with a scope one level deeper. When that level would be deeper
     * than [Limits.MAX_PATH_DEPTH], the block does not run and a [ViolationCode.TOO_DEEP] error is reported at
     * the current path instead.
     *
     * @param property the property that holds the nested value; only its name is used.
     * @param block the validation of the nested value.
     * @since 1.0.0
     */
    public fun within(property: KProperty<*>, block: ValidationScope.() -> Unit) {
        descend(prefix.name(property), block)
    }

    /**
     * Validates every element of a list, each below [property] and its position, such as `sinks[1]`.
     *
     * The [block] runs once per element, in iteration order, with a scope two levels deeper; the depth limit applies
     * as for [within]. Every element is visited, however many there are, so bound the size of a collection that comes
     * from input before it is validated.
     *
     * @param T the type of the elements.
     * @param property the property that holds the list; only its name is used.
     * @param items the elements to validate.
     * @param block the validation of one element.
     * @since 1.0.0
     */
    public fun <T> each(property: KProperty<*>, items: Iterable<T>, block: ValidationScope.(T) -> Unit) {
        val base = prefix.name(property)
        items.forEachIndexed { index, item -> descend(base.index(index)) { block(item) } }
    }

    /**
     * Validates every value of a map, each below [property] and its key, such as `labels["zone"]`.
     *
     * The [block] runs once per entry, in the iteration order of the map, with a scope two levels deeper; the depth
     * limit applies as for [within]. The keys may come from untrusted input; they are kept as given and escaped
     * wherever the path is shown. Every entry is visited, however many there are, so bound the size of a map that
     * comes from input before it is validated.
     *
     * @param V the type of the values.
     * @param property the property that holds the map; only its name is used.
     * @param entries the entries whose values are validated, in their iteration order.
     * @param block the validation of one value.
     * @since 1.0.0
     */
    public fun <V> each(property: KProperty<*>, entries: Map<String, V>, block: ValidationScope.(V) -> Unit) {
        val base = prefix.name(property)
        entries.forEach { (key, value) -> descend(base.key(key)) { block(value) } }
    }

    /**
     * Takes over violations another validation reported, each moved below the current path and [under].
     *
     * Code, severity and message stay as they are. The violations count against [Limits.MAX_VIOLATIONS] like
     * every other violation of this validation; their paths are not checked against [Limits.MAX_PATH_DEPTH].
     *
     * @param violations the violations to take over, in the order they are added.
     * @param under the path, relative to the current one, the violations are moved below; [ViolationPath.ROOT]
     * unless given.
     * @since 1.0.0
     */
    public fun include(violations: List<Violation>, under: ViolationPath = ViolationPath.ROOT) {
        val base = prefix + under
        violations.forEach { violation -> collector.add(violation.under(base)) }
    }

    internal fun violations(): List<Violation> = collector.violations()

    private fun descend(path: ViolationPath, block: ValidationScope.() -> Unit) {
        if (path.depth > Limits.MAX_PATH_DEPTH) {
            val message = "nests deeper than ${Limits.MAX_PATH_DEPTH} levels, so nothing below was validated"
            record(path = prefix, code = ViolationCode.TOO_DEEP, severity = Severity.Error, message = message)
            return
        }
        val nested = ValidationScope(path, defaultCode, collector)
        nested.block()
    }

    private fun record(path: ViolationPath, code: ViolationCode, severity: Severity, message: String) {
        collector.add(Violation(path = path, code = code, severity = severity, message = message))
    }
}
