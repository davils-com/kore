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

package com.davils.kore.core.validation.violation

/**
 * How severe a [KoreViolation] is: whether it makes the validated value invalid or only points something out.
 *
 * The name of the severity starts the text of a violation, such as `Error at 'name' (...)`.
 *
 * @since 1.0.0
 */
public enum class KoreSeverity {
    /**
     * Something questionable that does not make the value invalid.
     *
     * A report with warnings only is valid, and
     * [KoreValidationReport.throwIfInvalid][com.davils.kore.core.validation.report.KoreValidationReport.throwIfInvalid]
     * does not throw over it.
     *
     * @since 1.0.0
     */
    Warning,

    /**
     * A problem that makes the value invalid.
     *
     * A single error makes the report invalid, and
     * [KoreValidationReport.throwIfInvalid][com.davils.kore.core.validation.report.KoreValidationReport.throwIfInvalid]
     * then throws.
     *
     * @since 1.0.0
     */
    Error,
}
