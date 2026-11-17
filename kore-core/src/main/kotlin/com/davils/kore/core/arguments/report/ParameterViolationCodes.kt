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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The stable codes of the violations a [com.davils.kore.core.arguments.Parameters] schema reports.
 *
 * Each [ParameterViolation] carries one of them, except a [ParameterViolation.ValidationFailure], which carries
 * the code its rule reported and [VALIDATION_FAILED] by default. A program that reacts to a kind of problem compares
 * codes rather than messages, which may change.
 *
 * @since 1.0.0
 */
public object ParameterViolationCodes {
    /**
     * `kore.parameter.missing-required`: a required parameter received no value, see
     * [ParameterViolation.MissingRequired].
     *
     * @since 1.0.0
     */
    public val MISSING_REQUIRED: ViolationCode = ViolationCode("kore.parameter.missing-required")

    /**
     * `kore.parameter.missing-value`: a parameter that takes a value was not followed by one, see
     * [ParameterViolation.MissingValue].
     *
     * @since 1.0.0
     */
    public val MISSING_VALUE: ViolationCode = ViolationCode("kore.parameter.missing-value")

    /**
     * `kore.parameter.unknown-argument`: an argument names no declared parameter, see
     * [ParameterViolation.UnknownArgument].
     *
     * @since 1.0.0
     */
    public val UNKNOWN_ARGUMENT: ViolationCode = ViolationCode("kore.parameter.unknown-argument")

    /**
     * `kore.parameter.malformed-argument`: an argument does not follow the command line grammar, see
     * [ParameterViolation.MalformedArgument].
     *
     * @since 1.0.0
     */
    public val MALFORMED_ARGUMENT: ViolationCode = ViolationCode("kore.parameter.malformed-argument")

    /**
     * `kore.parameter.stray-value`: an argument before `--` belongs to no parameter, see
     * [ParameterViolation.StrayValue].
     *
     * @since 1.0.0
     */
    public val STRAY_VALUE: ViolationCode = ViolationCode("kore.parameter.stray-value")

    /**
     * `kore.parameter.conversion-failed`: a value could not be read as the type of its parameter, see
     * [ParameterViolation.ConversionFailure].
     *
     * @since 1.0.0
     */
    public val CONVERSION_FAILED: ViolationCode = ViolationCode("kore.parameter.conversion-failed")

    /**
     * `kore.parameter.validation-failed`: a validation rule refused a value without naming a code of its own, or failed
     * itself, see [ParameterViolation.ValidationFailure].
     *
     * @since 1.0.0
     */
    public val VALIDATION_FAILED: ViolationCode = ViolationCode("kore.parameter.validation-failed")

    /**
     * `kore.parameter.repeated`: a parameter that takes a single value was given more than once, see
     * [ParameterViolation.Repeated].
     *
     * @since 1.0.0
     */
    public val REPEATED: ViolationCode = ViolationCode("kore.parameter.repeated")

    /**
     * `kore.parameter.mutually-exclusive`: several parameters of an exclusive group received a value, see
     * [ParameterViolation.MutuallyExclusive].
     *
     * @since 1.0.0
     */
    public val MUTUALLY_EXCLUSIVE: ViolationCode = ViolationCode("kore.parameter.mutually-exclusive")

    /**
     * `kore.parameter.missing-dependency`: a parameter received a value but a parameter it requires did not, see
     * [ParameterViolation.MissingDependency].
     *
     * @since 1.0.0
     */
    public val MISSING_DEPENDENCY: ViolationCode = ViolationCode("kore.parameter.missing-dependency")

    /**
     * `kore.parameter.deprecated`: a deprecated parameter was given, see [ParameterViolation.Deprecated].
     *
     * @since 1.0.0
     */
    public val DEPRECATED: ViolationCode = ViolationCode("kore.parameter.deprecated")

    /**
     * `kore.parameter.source-unavailable`: a value source failed while it was asked for a value, see
     * [ParameterViolation.SourceUnavailable].
     *
     * @since 1.0.0
     */
    public val SOURCE_UNAVAILABLE: ViolationCode = ViolationCode("kore.parameter.source-unavailable")

    /**
     * `kore.parameter.sensitive-on-command-line`: a sensitive parameter was given on the command line, see
     * [ParameterViolation.SensitiveOnCommandLine].
     *
     * @since 1.0.0
     */
    public val SENSITIVE_ON_COMMAND_LINE: ViolationCode = ViolationCode(
        "kore.parameter.sensitive-on-command-line",
    )

    /**
     * `kore.parameter.too-many-arguments`: the command line has more arguments than the schema reads, see
     * [ParameterViolation.TooManyArguments].
     *
     * @since 1.0.0
     */
    public val TOO_MANY_ARGUMENTS: ViolationCode = ViolationCode("kore.parameter.too-many-arguments")

    /**
     * `kore.parameter.too-long`: an argument or a value is longer than the schema reads, see
     * [ParameterViolation.ArgumentTooLong] and [ParameterViolation.ValueTooLong].
     *
     * @since 1.0.0
     */
    public val TOO_LONG: ViolationCode = ViolationCode("kore.parameter.too-long")

    /**
     * `kore.parameter.too-many-values`: a parameter received more values than the schema reads, see
     * [ParameterViolation.TooManyValues].
     *
     * @since 1.0.0
     */
    public val TOO_MANY_VALUES: ViolationCode = ViolationCode("kore.parameter.too-many-values")
}
