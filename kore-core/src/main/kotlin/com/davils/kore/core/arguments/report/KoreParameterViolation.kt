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

import com.davils.kore.core.arguments.KoreParameterInfo
import com.davils.kore.core.arguments.conversion.KoreConversionException
import com.davils.kore.core.arguments.conversion.maskedUserInfo
import com.davils.kore.core.immutableList
import com.davils.kore.core.text.escape.SafeDisplay
import com.davils.kore.core.validation.violation.KoreSeverity
import com.davils.kore.core.validation.violation.KoreViolation
import com.davils.kore.core.validation.violation.KoreViolationCode
import com.davils.kore.core.validation.violation.KoreViolationPath

private const val WITHHELD_DETAIL: String = "failed validation - the detail is withheld because the parameter " +
    "is sensitive"
private const val NOT_GIVEN: String = "is required but was not given"
private const val UNKNOWN: String = "is not a known parameter"
private const val DEPRECATED_MESSAGE: String = "is deprecated"
private const val EXPOSED_MESSAGE: String = "is sensitive but was given on the command line, where the process " +
    "list and the shell history show it"
private const val NAME_SEPARATOR: String = ", "
private const val ARGUMENTS_LABEL: String = "arguments"

/**
 * One problem that a load of a [com.davils.kore.core.arguments.KoreParameters] schema found.
 *
 * A failed load throws a [KoreParameterException] with every violation, and a successful one keeps its warnings in
 * [com.davils.kore.core.arguments.KoreParameters.warnings]. A program can react to each subclass, which carries the
 * details of its kind of problem, or to its [code]. Violations are immutable and safe to share between threads; Kore
 * creates them, a program does not.
 *
 * The [message] is raw: it may contain text from the command line, the environment or a source, which an attacker may
 * control. A report and [toString] escape it, and code that writes the message anywhere itself escapes it as well; the
 * [path] escapes such text when it is printed. A value of a sensitive parameter never appears in either, and user
 * information in a URI is masked.
 *
 * @since 1.0.0
 */
public sealed class KoreParameterViolation {
    /**
     * Where the problem is: usually the primary name of the parameter, the name of the environment variable, or the
     * parameter followed by the source or `(default)` its value came from.
     *
     * @since 1.0.0
     */
    public abstract val path: KoreViolationPath

    /**
     * The stable name of the kind of problem, one of [KoreParameterViolationCodes] unless a validation rule chose its
     * own.
     *
     * @since 1.0.0
     */
    public abstract val code: KoreViolationCode

    /**
     * Whether the problem refuses the load; [KoreSeverity.Error] unless the subclass says otherwise.
     *
     * @since 1.0.0
     */
    public open val severity: KoreSeverity get() = KoreSeverity.Error

    /**
     * What is wrong, as a phrase that follows the [path], such as `is required but was not given`; raw and not escaped.
     *
     * @since 1.0.0
     */
    public abstract val message: String

    internal fun toViolation(): KoreViolation =
        KoreViolation(path = path, code = code, severity = severity, message = message)

    /**
     * Returns the violation as one line with its severity, path, code and message, escaped and shortened, so it is safe
     * to log.
     *
     * @return the violation as text.
     * @since 1.0.0
     */
    override fun toString(): String = toViolation().toString()

    /**
     * A required parameter received no value from the command line, the environment or a source.
     *
     * The message names the environment variable of the parameter when it has one.
     *
     * @property parameter the parameter that received no value.
     * @since 1.0.0
     */
    public class MissingRequired internal constructor(
        public val parameter: KoreParameterInfo,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = KoreViolationPath.named(parameter.primaryName)

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.MISSING_REQUIRED

        override val message: String
            get() {
                val variable = parameter.environmentVariable ?: return NOT_GIVEN
                return "$NOT_GIVEN, and $variable is not set"
            }
    }

    /**
     * A parameter that takes a value was the last argument, or was followed by an argument that does not look like a
     * value, such as another parameter.
     *
     * An argument that starts with a dash is taken as a value only when it is a single dash or a plain ASCII number; a
     * value that starts with a dash is given in the form `--name=value` or `-n=value` instead. When the parameter is
     * sensitive, the argument that followed it may be the secret itself, so it is skipped: it is neither read as a
     * parameter nor reported, collected or counted.
     *
     * @property parameter the parameter that is missing its value.
     * @property token the name under which the parameter was given: the primary name when it was given by a long name,
     * and the short name when it was given by one.
     * @since 1.0.0
     */
    public class MissingValue internal constructor(
        public val parameter: KoreParameterInfo,
        public val token: String,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = tokenPath(token)

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.MISSING_VALUE

        override val message: String get() = "expects a value of type ${parameter.typeName} but none followed"
    }

    /**
     * An argument before `--` names no declared parameter, in a schema that uses
     * [com.davils.kore.core.arguments.KoreUnknownArgumentPolicy.Reject].
     *
     * The violation never contains the value written into the argument, so `--passwd=secret` is reported as `--passwd`.
     *
     * @property token the unknown name as it was given, such as `--passwd`, or the first undeclared letter of a bundle
     * of short flags, such as `-x`; it comes from the command line and is not escaped.
     * @property suggestion a declared name that is close to [token], to be offered as `did you mean`, or `null` when
     * there is none. It is searched only for an unknown long name of at most 64 characters, and only for the first
     * [com.davils.kore.core.arguments.KoreParameterLimits.maxSuggestedArguments] of them.
     * @property bundlePosition the position of the argument, counted from 1, when [token] is part of a bundle of
     * several short flags, none of which was applied; `null` otherwise.
     * @since 1.0.0
     */
    public class UnknownArgument internal constructor(
        public val token: String,
        public val suggestion: String?,
        public val bundlePosition: Int?,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = tokenPath(token)

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.UNKNOWN_ARGUMENT

        override val message: String get() = bundleMessage() ?: suggestionMessage()

        private fun bundleMessage(): String? {
            val position = bundlePosition ?: return null
            return "$UNKNOWN, so nothing in argument $position was applied"
        }

        private fun suggestionMessage(): String {
            val candidate = suggestion ?: return UNKNOWN
            return "$UNKNOWN - did you mean $candidate?"
        }
    }

    /**
     * An argument does not follow the command line grammar, such as `--=value` without a name or a negated flag that is
     * given a value.
     *
     * @property token the part of the argument before its `=`, as it was given; it comes from the command line and is
     * not escaped.
     * @property reason why the argument is malformed, as constant text.
     * @since 1.0.0
     */
    public class MalformedArgument internal constructor(
        public val token: String,
        public val reason: String,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = tokenPath(token)

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.MALFORMED_ARGUMENT

        override val message: String get() = reason
    }

    /**
     * An argument before `--` is neither a parameter nor the value of one, in a schema that uses
     * [com.davils.kore.core.arguments.KoreUnknownArgumentPolicy.Reject].
     *
     * The violation names only the position of the argument, never its text. A program that takes positional arguments
     * expects them after `--`.
     *
     * @property position the position of the argument, counted from 1.
     * @since 1.0.0
     */
    public class StrayValue internal constructor(public val position: Int) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = KoreViolationPath.named("argument $position")

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.STRAY_VALUE

        override val message: String get() = "is a value with no parameter to belong to"
    }

    /**
     * A value could not be read as the type of its parameter, or a default no longer passes the converter that checks
     * it.
     *
     * For a sensitive parameter the [value] is `****`, the [reason] is the constant `could not be read as` followed by
     * the type name, and the [cause] is a [KoreRedactedException]. For any other parameter the [value] is the text as
     * given with the user information of a URI masked, and the [reason] is the message of the
     * [KoreConversionException] that the converter threw, or the constant reason when it threw anything else; its
     * [cause] is kept as thrown and may repeat the value in its own message, which no report renders. The violation
     * decides this itself when it is created, so it never holds the text or the exception that was given for a
     * sensitive parameter.
     *
     * @since 1.0.0
     */
    public class ConversionFailure internal constructor(
        /**
         * The parameter whose value could not be read.
         *
         * @since 1.0.0
         */
        public val parameter: KoreParameterInfo,
        /**
         * Where the value came from.
         *
         * @since 1.0.0
         */
        public val origin: KoreValueOrigin,
        given: String,
        thrown: Throwable,
    ) : KoreParameterViolation() {
        /**
         * The value as it is shown: `****` for a sensitive parameter, and otherwise the text as given with the user
         * information of a URI masked; raw apart from the masking, and not escaped.
         *
         * @since 1.0.0
         */
        public val value: String = shownValue(parameter, given)

        /**
         * Why the value could not be read, phrased to follow the quoted value.
         *
         * @since 1.0.0
         */
        public val reason: String = conversionReason(parameter, thrown)

        /**
         * What the converter threw, or a [KoreRedactedException] in its place for a sensitive parameter.
         *
         * @since 1.0.0
         */
        public val cause: Throwable = redactedFor(parameter.isSensitive, thrown)

        override val path: KoreViolationPath get() = KoreViolationPath.named(origin.label(parameter))

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.CONVERSION_FAILED

        override val message: String get() = "'$value' $reason"
    }

    /**
     * A validation rule of a parameter refused or questioned a value, or failed itself.
     *
     * The [code] and the [severity] are those the rule reported, so a warning of a rule ends up in
     * [com.davils.kore.core.arguments.KoreParameters.warnings] instead of refusing the load. The [message] is what the
     * rule reported; for a sensitive parameter it is replaced by a constant text that withholds it, and what the rule
     * reported is dropped when the violation is created, so no field or method of the violation holds it. A rule that
     * throws is reported with the code [KoreParameterViolationCodes.VALIDATION_FAILED], the severity
     * [KoreSeverity.Error] and a constant message, and what it threw is not kept.
     *
     * @since 1.0.0
     */
    public class ValidationFailure internal constructor(
        /**
         * The parameter whose value was validated.
         *
         * @since 1.0.0
         */
        public val parameter: KoreParameterInfo,
        /**
         * Where the validated value came from, [KoreValueOrigin.Default] for a default.
         *
         * @since 1.0.0
         */
        public val origin: KoreValueOrigin,
        detail: String,
        override val code: KoreViolationCode,
        override val severity: KoreSeverity,
    ) : KoreParameterViolation() {
        override val message: String = WITHHELD_DETAIL.takeIf { parameter.isSensitive } ?: detail

        override val path: KoreViolationPath get() = KoreViolationPath.named(origin.label(parameter))
    }

    /**
     * A [com.davils.kore.core.arguments.KoreValueSource] threw while it was asked for the value of a parameter, or
     * while its name was read.
     *
     * It refuses the load even when the parameter is optional. The message is constant; the [cause] of a parameter that
     * is not sensitive is kept as thrown, may contain whatever the source put into it, and is never rendered by a
     * report. The violation replaces the cause of a sensitive parameter itself when it is created, so it never holds
     * what the source threw for one.
     *
     * @since 1.0.0
     */
    public class SourceUnavailable internal constructor(
        /**
         * The parameter whose value was asked for.
         *
         * @since 1.0.0
         */
        public val parameter: KoreParameterInfo,
        /**
         * The name of the source that failed, or `source` followed by the position of the source in the list given to
         * `load`, counted from 1, such as `source 2`, when reading its name threw.
         *
         * @since 1.0.0
         */
        public val sourceName: String,
        thrown: Throwable,
    ) : KoreParameterViolation() {
        /**
         * What the source threw, or a [KoreRedactedException] in its place for a sensitive parameter.
         *
         * @since 1.0.0
         */
        public val cause: Throwable = redactedFor(parameter.isSensitive, thrown)

        override val path: KoreViolationPath
            get() = KoreViolationPath.named(KoreValueOrigin.External(sourceName).label(parameter))

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.SOURCE_UNAVAILABLE

        override val message: String get() = "could not be read, because the source failed"
    }

    /**
     * A parameter that takes a single value was given more than once on the command line.
     *
     * @property parameter the parameter that was repeated.
     * @property occurrences how often it was given, counted in full even beyond
     * [com.davils.kore.core.arguments.KoreParameterLimits.maxValuesPerParameter], and at most [Int.MAX_VALUE].
     * @since 1.0.0
     */
    public class Repeated internal constructor(
        public val parameter: KoreParameterInfo,
        public val occurrences: Int,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = KoreViolationPath.named(parameter.primaryName)

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.REPEATED

        override val message: String get() = "was given $occurrences times but accepts a single value"
    }

    /**
     * More than one parameter of an exclusive group received a value, from any place; a default does not count.
     *
     * @since 1.0.0
     */
    public class MutuallyExclusive internal constructor(
        parameters: List<KoreParameterInfo>,
    ) : KoreParameterViolation() {
        /**
         * The parameters of the group that received a value, in the order the group names them; the list cannot be
         * changed.
         *
         * @since 1.0.0
         */
        public val parameters: List<KoreParameterInfo> = parameters.immutableList()

        override val path: KoreViolationPath
            get() = KoreViolationPath.named(parameters.joinToString(NAME_SEPARATOR) { it.primaryName })

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.MUTUALLY_EXCLUSIVE

        override val message: String get() = "cannot be used together"
    }

    /**
     * A parameter received a value, but a parameter it requires did not; a default does not count.
     *
     * @property parameter the parameter that received a value.
     * @since 1.0.0
     */
    public class MissingDependency internal constructor(
        public val parameter: KoreParameterInfo,
        missing: List<KoreParameterInfo>,
    ) : KoreParameterViolation() {
        /**
         * The required parameters that received no value, in the order the dependency names them; the list cannot be
         * changed.
         *
         * @since 1.0.0
         */
        public val missing: List<KoreParameterInfo> = missing.immutableList()

        override val path: KoreViolationPath get() = KoreViolationPath.named(parameter.primaryName)

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.MISSING_DEPENDENCY

        override val message: String get() = "requires ${missing.joinToString(NAME_SEPARATOR) { it.primaryName }}"
    }

    /**
     * A deprecated parameter was given, on the command line, in the environment or by a source; this is a
     * [KoreSeverity.Warning].
     *
     * @property parameter the deprecated parameter.
     * @property advice what to use instead, as the declaration says; empty when it gives no advice.
     * @since 1.0.0
     */
    public class Deprecated internal constructor(
        public val parameter: KoreParameterInfo,
        public val advice: String,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = KoreViolationPath.named(parameter.primaryName)

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.DEPRECATED

        override val severity: KoreSeverity get() = KoreSeverity.Warning

        override val message: String
            get() = DEPRECATED_MESSAGE.takeIf { advice.isEmpty() } ?: "$DEPRECATED_MESSAGE: $advice"
    }

    /**
     * A sensitive parameter was given on the command line, where the process list and the shell history show it to
     * other users of the machine.
     *
     * The message advises to set the environment variable of the parameter instead, when it has one. The value is read
     * nonetheless, unless the schema makes this an error.
     *
     * @property parameter the sensitive parameter.
     * @property severity [KoreSeverity.Warning] unless the schema was created with another severity for this case.
     * @since 1.0.0
     */
    public class SensitiveOnCommandLine internal constructor(
        public val parameter: KoreParameterInfo,
        override val severity: KoreSeverity,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = KoreViolationPath.named(parameter.primaryName)

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.SENSITIVE_ON_COMMAND_LINE

        override val message: String
            get() {
                val variable = parameter.environmentVariable ?: return EXPOSED_MESSAGE
                return "$EXPOSED_MESSAGE; set $variable instead"
            }
    }

    /**
     * The command line has more arguments than the schema reads, so none of them was read.
     *
     * It is the only violation of such a load.
     *
     * @property count how many arguments were given.
     * @property limit the most arguments the schema reads,
     * [com.davils.kore.core.arguments.KoreParameterLimits.maxArguments].
     * @since 1.0.0
     */
    public class TooManyArguments internal constructor(
        public val count: Int,
        public val limit: Int,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = KoreViolationPath.named(ARGUMENTS_LABEL)

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.TOO_MANY_ARGUMENTS

        override val message: String get() = "are $count, more than the $limit this program reads, so none was read"
    }

    /**
     * An argument before `--` is longer than the schema reads, so no argument was read.
     *
     * Every such argument is reported, by its position only, never by its text.
     *
     * @property position the position of the argument, counted from 1.
     * @property limit the longest argument the schema reads,
     * [com.davils.kore.core.arguments.KoreParameterLimits.maxArgumentLength].
     * @since 1.0.0
     */
    public class ArgumentTooLong internal constructor(
        public val position: Int,
        public val limit: Int,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = KoreViolationPath.named("argument $position")

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.TOO_LONG

        override val message: String get() = tooLongMessage(limit)
    }

    /**
     * A value from the environment or a source is longer than the schema reads, so it was not read.
     *
     * @property parameter the parameter the value was meant for.
     * @property origin where the value came from.
     * @property limit the longest value the schema reads,
     * [com.davils.kore.core.arguments.KoreParameterLimits.maxArgumentLength].
     * @since 1.0.0
     */
    public class ValueTooLong internal constructor(
        public val parameter: KoreParameterInfo,
        public val origin: KoreValueOrigin,
        public val limit: Int,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = KoreViolationPath.named(origin.label(parameter))

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.TOO_LONG

        override val message: String get() = tooLongMessage(limit)
    }

    /**
     * A parameter received more values than the schema reads, counted over all its occurrences after splitting, or was
     * given on the command line more often than that, so none of them was read.
     *
     * @property parameter the parameter that received too many values.
     * @property origin where the values came from.
     * @property limit the most values the schema reads for one parameter,
     * [com.davils.kore.core.arguments.KoreParameterLimits.maxValuesPerParameter].
     * @since 1.0.0
     */
    public class TooManyValues internal constructor(
        public val parameter: KoreParameterInfo,
        public val origin: KoreValueOrigin,
        public val limit: Int,
    ) : KoreParameterViolation() {
        override val path: KoreViolationPath get() = KoreViolationPath.named(origin.label(parameter))

        override val code: KoreViolationCode get() = KoreParameterViolationCodes.TOO_MANY_VALUES

        override val message: String get() = "has more than $limit values, so none of them was read"
    }
}

private fun shownValue(parameter: KoreParameterInfo, given: String): String {
    if (parameter.isSensitive) return SafeDisplay.REDACTED
    return maskedUserInfo(given)
}

private fun conversionReason(parameter: KoreParameterInfo, thrown: Throwable): String {
    if (thrown is KoreConversionException && !parameter.isSensitive) return thrown.message
    return "could not be read as ${parameter.typeName}"
}

private fun tokenPath(token: String): KoreViolationPath {
    if (token.isEmpty()) return KoreViolationPath.keyed(token)
    return KoreViolationPath.named(token)
}

private fun tooLongMessage(limit: Int): String = "is longer than $limit characters, so it was not read"
