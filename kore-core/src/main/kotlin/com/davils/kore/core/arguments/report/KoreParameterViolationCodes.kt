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

package com.davils.kore.core.arguments.report

import com.davils.kore.core.validation.violation.KoreViolationCode

/**
 * The stable codes of the violations a [com.davils.kore.core.arguments.KoreParameters] schema reports.
 *
 * Each [KoreParameterViolation] carries one of them, except a [KoreParameterViolation.ValidationFailure], which carries
 * the code its rule reported and [VALIDATION_FAILED] by default. A program that reacts to a kind of problem compares
 * codes rather than messages, which may change.
 *
 * @since 1.0.0
 */
public object KoreParameterViolationCodes {
    /**
     * `kore.parameter.missing-required`: a required parameter received no value, see
     * [KoreParameterViolation.MissingRequired].
     *
     * @since 1.0.0
     */
    public val MISSING_REQUIRED: KoreViolationCode = KoreViolationCode("kore.parameter.missing-required")

    /**
     * `kore.parameter.missing-value`: a parameter that takes a value was not followed by one, see
     * [KoreParameterViolation.MissingValue].
     *
     * @since 1.0.0
     */
    public val MISSING_VALUE: KoreViolationCode = KoreViolationCode("kore.parameter.missing-value")

    /**
     * `kore.parameter.unknown-argument`: an argument names no declared parameter, see
     * [KoreParameterViolation.UnknownArgument].
     *
     * @since 1.0.0
     */
    public val UNKNOWN_ARGUMENT: KoreViolationCode = KoreViolationCode("kore.parameter.unknown-argument")

    /**
     * `kore.parameter.malformed-argument`: an argument does not follow the command line grammar, see
     * [KoreParameterViolation.MalformedArgument].
     *
     * @since 1.0.0
     */
    public val MALFORMED_ARGUMENT: KoreViolationCode = KoreViolationCode("kore.parameter.malformed-argument")

    /**
     * `kore.parameter.stray-value`: an argument before `--` belongs to no parameter, see
     * [KoreParameterViolation.StrayValue].
     *
     * @since 1.0.0
     */
    public val STRAY_VALUE: KoreViolationCode = KoreViolationCode("kore.parameter.stray-value")

    /**
     * `kore.parameter.conversion-failed`: a value could not be read as the type of its parameter, see
     * [KoreParameterViolation.ConversionFailure].
     *
     * @since 1.0.0
     */
    public val CONVERSION_FAILED: KoreViolationCode = KoreViolationCode("kore.parameter.conversion-failed")

    /**
     * `kore.parameter.validation-failed`: a validation rule refused a value without naming a code of its own, or failed
     * itself, see [KoreParameterViolation.ValidationFailure].
     *
     * @since 1.0.0
     */
    public val VALIDATION_FAILED: KoreViolationCode = KoreViolationCode("kore.parameter.validation-failed")

    /**
     * `kore.parameter.repeated`: a parameter that takes a single value was given more than once, see
     * [KoreParameterViolation.Repeated].
     *
     * @since 1.0.0
     */
    public val REPEATED: KoreViolationCode = KoreViolationCode("kore.parameter.repeated")

    /**
     * `kore.parameter.mutually-exclusive`: several parameters of an exclusive group received a value, see
     * [KoreParameterViolation.MutuallyExclusive].
     *
     * @since 1.0.0
     */
    public val MUTUALLY_EXCLUSIVE: KoreViolationCode = KoreViolationCode("kore.parameter.mutually-exclusive")

    /**
     * `kore.parameter.missing-dependency`: a parameter received a value but a parameter it requires did not, see
     * [KoreParameterViolation.MissingDependency].
     *
     * @since 1.0.0
     */
    public val MISSING_DEPENDENCY: KoreViolationCode = KoreViolationCode("kore.parameter.missing-dependency")

    /**
     * `kore.parameter.deprecated`: a deprecated parameter was given, see [KoreParameterViolation.Deprecated].
     *
     * @since 1.0.0
     */
    public val DEPRECATED: KoreViolationCode = KoreViolationCode("kore.parameter.deprecated")

    /**
     * `kore.parameter.source-unavailable`: a value source failed while it was asked for a value, see
     * [KoreParameterViolation.SourceUnavailable].
     *
     * @since 1.0.0
     */
    public val SOURCE_UNAVAILABLE: KoreViolationCode = KoreViolationCode("kore.parameter.source-unavailable")

    /**
     * `kore.parameter.sensitive-on-command-line`: a sensitive parameter was given on the command line, see
     * [KoreParameterViolation.SensitiveOnCommandLine].
     *
     * @since 1.0.0
     */
    public val SENSITIVE_ON_COMMAND_LINE: KoreViolationCode = KoreViolationCode(
        "kore.parameter.sensitive-on-command-line",
    )

    /**
     * `kore.parameter.too-many-arguments`: the command line has more arguments than the schema reads, see
     * [KoreParameterViolation.TooManyArguments].
     *
     * @since 1.0.0
     */
    public val TOO_MANY_ARGUMENTS: KoreViolationCode = KoreViolationCode("kore.parameter.too-many-arguments")

    /**
     * `kore.parameter.too-long`: an argument or a value is longer than the schema reads, see
     * [KoreParameterViolation.ArgumentTooLong] and [KoreParameterViolation.ValueTooLong].
     *
     * @since 1.0.0
     */
    public val TOO_LONG: KoreViolationCode = KoreViolationCode("kore.parameter.too-long")

    /**
     * `kore.parameter.too-many-values`: a parameter received more values than the schema reads, see
     * [KoreParameterViolation.TooManyValues].
     *
     * @since 1.0.0
     */
    public val TOO_MANY_VALUES: KoreViolationCode = KoreViolationCode("kore.parameter.too-many-values")
}
