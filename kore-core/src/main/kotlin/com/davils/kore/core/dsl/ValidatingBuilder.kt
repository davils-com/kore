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

package com.davils.kore.core.dsl

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.ViolationCollector
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.report.ValidationReport
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.Violation
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.validation.violation.ViolationPath
import kotlin.reflect.KProperty

private const val DEFAULT_SUBJECT: String = "configuration"
private const val DUPLICATE_MESSAGE: String = "is declared more than once"
private const val EVALUATED_MESSAGE: String =
    "This builder was already evaluated by build() or validate(), so nothing in it can be changed any more"
private const val UNFINISHED_MESSAGE: String =
    "This builder is still being evaluated or its evaluation failed, so it cannot be evaluated again"

/**
 * A builder for a DSL that creates a [Validatable] value, validates it together with every nested block and
 * reports all problems of all levels in one [ValidationReport].
 *
 * A subclass holds the state that a block of the DSL sets and implements the protected `assemble()`, which creates
 * the value from that state; the validation of the value does the checking. `assemble()` may also refuse to create
 * the value by throwing a [ValidationException], such as when it creates the value through a factory that
 * validates it: the violations of that exception then join the report at the paths they name, and the builder has
 * no value. A nested block has a builder of its own. The functions that declare one run the block at once, evaluate
 * the nested builder and file its problems under the name of `property`, the property of the built value that the
 * block fills. The protected members a subclass works with are:
 *
 * - `nested(builder, property, block)` returns the value of a single nested block. A second block for the same
 *   property is not run; it is reported as [ViolationCode.DUPLICATE] and the first value is kept.
 * - `collect(target, builder, property, block)` adds the value of one block of a list to `target`. The blocks of a
 *   path are numbered in the order they are declared, so a block keeps its position, such as `sinks[2]`, even when
 *   a block before it failed.
 * - `collect(target, key, builder, property, block)` puts the value of one block of a map into `target` under `key`.
 *   A second block for the same key is not run; it is reported as [ViolationCode.DUPLICATE]. The key may come
 *   from untrusted input; it is escaped wherever the path is shown.
 * - `adopt(violations, path)` takes over violations that were found elsewhere, moved below `path`.
 * - `checkOpen()` throws an [IllegalStateException] once the builder was evaluated. A subclass calls it first in every
 *   setter and function that changes its state, so that a change after the evaluation fails instead of being lost.
 * - `subject` names the built value in the report; it is `configuration` unless a subclass overrides it.
 *
 * A nested block that has an error leaves no value: `nested` returns null and `collect` adds nothing, so
 * `assemble()` sees only valid nested values and must cope with a missing one, while the errors of the nested block
 * are reported under its path all the same. Warnings alone keep no value out, as long as a validation finds at most
 * [Limits.MAX_VIOLATIONS] problems: beyond that, the [ViolationCode.TOO_MANY] violation that counts the rest
 * is an error, whatever the severity of the problems it counts, so too many warnings keep a value out as well.
 *
 * The first call of [build] or [validate] evaluates the builder: it runs `assemble()` once, validates the value and
 * keeps the report, which holds at most [Limits.MAX_VIOLATIONS] violations and counts the rest, together with the
 * value when the report holds no error. The builder keeps no more than that many adopted violations either while
 * blocks are declared, however many it is given. Every later call answers with the same value and the same report.
 * From then on the builder is closed: declaring a nested block, adopting violations and every change that the
 * subclass guards with `checkOpen()` throw an [IllegalStateException]; a change that it does not guard no longer has
 * any effect. Evaluating the builder from inside its own `assemble()`, or again after an evaluation threw, also
 * throws an [IllegalStateException]. Any other exception thrown by `assemble()`, by the validation of the value or by
 * a nested block propagates unchanged.
 *
 * A builder is not thread-safe. It is meant to be created, configured and evaluated by one thread, for one value.
 *
 * @param T the type of the value built.
 * @since 1.0.0
 */
@DavilsDsl
public abstract class ValidatingBuilder<T : Validatable> : Builder<T> {
    private val adopted = ViolationCollector()
    private val declaredIndices = mutableMapOf<ViolationPath, Int>()
    private val declaredPaths = mutableSetOf<ViolationPath>()
    private var evaluation: BuilderEvaluation<T>? = null
    private var isEvaluating: Boolean = false

    /**
     * Names the built value in the report, such as `server configuration`; `configuration` unless overridden.
     *
     * @since 1.0.0
     */
    protected open val subject: String get() = DEFAULT_SUBJECT

    /**
     * Creates the value from the state the block of the DSL set; it runs exactly once, at the first evaluation.
     *
     * It may throw a [com.davils.kore.core.validation.report.ValidationException]; its violations then join the
     * report and the builder has no value. A nested value that had an error is missing, so `assemble` must cope with
     * a null from [nested] and with fewer values from [collect].
     *
     * @return the built value, which is validated afterwards.
     * @since 1.0.0
     */
    protected abstract fun assemble(): T

    /**
     * Returns the value of this builder after checking that it, and every nested block, has no errors.
     *
     * The first call evaluates the builder and closes it, as described at [ValidatingBuilder]; every later call
     * returns the same value or throws with the same report. Warnings alone never make it throw, unless there are more
     * than [Limits.MAX_VIOLATIONS] problems, which always count as an error.
     *
     * @return the validated value.
     * @throws ValidationException if the value or a nested block has an error, or if `assemble()` refused to
     * create the value; its report names every problem.
     * @throws IllegalStateException if this builder is evaluated from inside its own `assemble()`, or again after an
     * earlier evaluation threw.
     * @since 1.0.0
     */
    final override fun build(): T = evaluate().valueOrThrow()

    /**
     * Returns the report of every problem of this builder and its nested blocks, without throwing for them.
     *
     * The first call evaluates the builder and closes it, as described at [ValidatingBuilder]; every later call,
     * and every call of [build], uses the same evaluation, so the report is the same object every time.
     *
     * @return the report; it is valid when it holds no errors.
     * @throws IllegalStateException if this builder is evaluated from inside its own `assemble()`, or again after an
     * earlier evaluation threw.
     * @since 1.0.0
     */
    public fun validate(): ValidationReport = evaluate().report

    /**
     * Runs a nested block at once and returns its value, filing its problems under the name of [property].
     *
     * A second block for the same property is not run; it is reported as [ViolationCode.DUPLICATE] and the first
     * value is kept.
     *
     * @param D the type of the nested value.
     * @param B the type of the nested builder.
     * @param builder a fresh builder for the nested block.
     * @param property the property of the built value that the block fills.
     * @param block the nested block of the DSL.
     * @return the nested value, or `null` when the block had an error or was a duplicate.
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    protected fun <D : Validatable, B : ValidatingBuilder<D>> nested(
        builder: B,
        property: KProperty<*>,
        block: B.() -> Unit,
    ): D? {
        val path = ViolationPath.named(property)
        if (!declare(path)) return null
        return nestedAt(builder, path, block)
    }

    /**
     * Runs one block of a list at once and adds its value to [target], filing its problems under its position, such
     * as `sinks[2]`.
     *
     * The blocks of a list are numbered in the order they are declared, so a block keeps its position even when a
     * block before it failed.
     *
     * @param D the type of an element of the list.
     * @param B the type of the nested builder.
     * @param target the list the value is added to; nothing is added when the block had an error.
     * @param builder a fresh builder for the block.
     * @param property the property of the built value that holds the list.
     * @param block the nested block of the DSL.
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    protected fun <D : Validatable, B : ValidatingBuilder<D>> collect(
        target: MutableList<in D>,
        builder: B,
        property: KProperty<*>,
        block: B.() -> Unit,
    ) {
        checkOpen()
        val listPath = ViolationPath.named(property)
        val index = declaredIndices.getOrDefault(listPath, 0)
        declaredIndices[listPath] = index + 1
        val value = nestedAt(builder, listPath.index(index), block) ?: return
        target += value
    }

    /**
     * Runs one block of a map at once and puts its value into [target] under [key], filing its problems under the
     * key, such as `labels["zone"]`.
     *
     * A second block for the same key is not run; it is reported as [ViolationCode.DUPLICATE]. The key may come
     * from untrusted input; it is escaped wherever the path is shown.
     *
     * @param D the type of a value of the map.
     * @param B the type of the nested builder.
     * @param target the map the value is put into; nothing is put when the block had an error or was a duplicate.
     * @param key the key of the entry.
     * @param builder a fresh builder for the block.
     * @param property the property of the built value that holds the map.
     * @param block the nested block of the DSL.
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    protected fun <D : Validatable, B : ValidatingBuilder<D>> collect(
        target: MutableMap<String, in D>,
        key: String,
        builder: B,
        property: KProperty<*>,
        block: B.() -> Unit,
    ) {
        val path = ViolationPath.named(property).key(key)
        if (!declare(path)) return
        val value = nestedAt(builder, path, block) ?: return
        target[key] = value
    }

    /**
     * Takes over violations that were found elsewhere, each moved below [path], so they join the report.
     *
     * @param violations the violations to take over.
     * @param path the path, relative to this builder, the violations are moved below.
     * @throws IllegalStateException if this builder was evaluated already.
     * @since 1.0.0
     */
    protected fun adopt(violations: List<Violation>, path: ViolationPath) {
        checkOpen()
        violations.forEach { violation -> adopted.add(violation.under(path)) }
    }

    /**
     * Throws when this builder was evaluated already, so that a change made after the evaluation fails instead of
     * being lost.
     *
     * A subclass calls it first in every setter and function that changes its state.
     *
     * @throws IllegalStateException if [build] or [validate] has evaluated this builder.
     * @since 1.0.0
     */
    protected fun checkOpen() {
        check(evaluation == null) { EVALUATED_MESSAGE }
    }

    internal fun evaluate(): BuilderEvaluation<T> {
        val cached = evaluation
        if (cached != null) return cached
        check(!isEvaluating) { UNFINISHED_MESSAGE }
        isEvaluating = true
        val scope = ValidationScope(ViolationPath.ROOT, ViolationCode.INVALID, adopted)
        val value = assembledInto(scope)
        if (value != null) with(value) { scope.validate() }
        val evaluated = BuilderEvaluation(value, ValidationReport(subject, scope.violations()))
        evaluation = evaluated
        return evaluated
    }

    private fun assembledInto(scope: ValidationScope): T? = try {
        assemble()
    } catch (refusal: ValidationException) {
        scope.include(refusal.report.violations)
        null
    }

    private fun declare(path: ViolationPath): Boolean {
        checkOpen()
        if (declaredPaths.add(path)) return true
        val duplicate = Violation(
            path = path,
            code = ViolationCode.DUPLICATE,
            severity = Severity.Error,
            message = DUPLICATE_MESSAGE,
        )
        adopted.add(duplicate)
        return false
    }

    private fun <D : Validatable, B : ValidatingBuilder<D>> nestedAt(
        builder: B,
        path: ViolationPath,
        block: B.() -> Unit,
    ): D? {
        builder.block()
        val evaluated = builder.evaluate()
        adopt(evaluated.report.violations, path)
        return evaluated.value
    }
}
