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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.validation.KoreValidationScope

/**
 * The settings of a parameter that takes a value, in addition to those of [KoreParameterConfiguration].
 *
 * @param T the type of the values of the parameter.
 * @since 1.0.0
 */
@DavilsDsl
public open class KoreValueConfiguration<T : Any> internal constructor() : KoreParameterConfiguration() {
    private var isSensitive: Boolean = false
    private val rules: MutableList<KoreValidationScope.(T) -> Unit> = mutableListOf()

    internal val validators: List<KoreValidationScope.(T) -> Unit> get() = rules.toList()

    /**
     * Marks the value of the parameter as a secret, such as a password or a token.
     *
     * A sensitive value never appears in a message, a reason, a cause, a `toString`, the usage or
     * [com.davils.kore.core.arguments.KoreParameters.describe]; it is shown only as `****`, which reveals neither its
     * length nor how many values there are. A conversion failure keeps a constant reason and replaces the exception of
     * the converter with a [com.davils.kore.core.arguments.report.KoreRedactedException], a failing source keeps only
     * the class name of its exception as well, and the messages of the validation rules are withheld. Giving a
     * sensitive value on the command line, where the process list and the shell history show it, is reported as
     * [com.davils.kore.core.arguments.report.KoreParameterViolation.SensitiveOnCommandLine]. Kore never makes a
     * parameter sensitive because of its name; only this call does.
     *
     * @since 1.0.0
     */
    public fun sensitive() {
        isSensitive = true
    }

    /**
     * Adds a validation rule that checks every converted value of the parameter, and its default.
     *
     * The rules run in the order they were added, all of them on every value, and report through the
     * [KoreValidationScope] they receive: a failure refuses the load with a
     * [com.davils.kore.core.arguments.report.KoreParameterViolation.ValidationFailure], and a warning ends up in
     * [com.davils.kore.core.arguments.KoreParameters.warnings]. Their messages may contain the value and are escaped
     * when shown, except for a sensitive parameter, whose messages are withheld. A rule that throws is contained: the
     * value counts as invalid and the violation has a constant message and no cause, while an [InterruptedException], a
     * [kotlin.coroutines.cancellation.CancellationException] or an [Error] escapes the load. A rule runs on the thread
     * that loads the schema and must not keep the scope.
     *
     * @param rule the check of one value.
     * @since 1.0.0
     */
    public fun validate(rule: KoreValidationScope.(T) -> Unit) {
        rules += rule
    }

    internal fun valueTraits(): ParameterTraits = traitsWith(isSensitive = isSensitive, isNegatable = false)
}
