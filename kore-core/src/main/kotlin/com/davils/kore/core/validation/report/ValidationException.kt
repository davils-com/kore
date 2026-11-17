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

package com.davils.kore.core.validation.report

/**
 * Thrown when a value that must be valid has errors; the [report] names every one of them.
 *
 * Kore throws it from [ValidationReport.throwIfInvalid] and the functions built on it, such as
 * [validateOrThrow][com.davils.kore.core.validation.validateOrThrow]; areas of Kore throw subclasses with more detail,
 * such as [ParameterException][com.davils.kore.core.arguments.report.ParameterException]. Its constructor is
 * internal, so Kotlin code outside Kore can neither create nor subclass it. It has no cause.
 *
 * The [message] is the rendered report, built once when it is first read. It is escaped and bounded in size as
 * [ValidationReport.render] describes, so it is safe to log; it shows the messages of the violations escaped but
 * never masked. The report keeps the violations exactly as the validation wrote them, so code that handles the
 * exception reads them through [report] and escapes what it shows.
 *
 * Java serialization writes only the rendered message and the stack trace: a deserialized failure is a plain
 * [RuntimeException] that carries both, never a [ValidationException] and never the violations themselves.
 *
 * @property report the report of the failed validation.
 * @since 1.0.0
 */
public open class ValidationException internal constructor(
    @Transient public val report: ValidationReport,
) : RuntimeException() {
    private val rendered: String by lazy { report.render() }

    /**
     * The rendered [report], escaped and bounded in size; it is never null.
     *
     * @since 1.0.0
     */
    override val message: String get() = rendered

    protected fun writeReplace(): Any = SerializedValidationFailure(rendered, stackTrace)
}
