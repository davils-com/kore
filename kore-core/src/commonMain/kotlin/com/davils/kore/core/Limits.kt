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

package com.davils.kore.core

/**
 * The upper bounds Kore enforces on everything it reads from outside the program.
 *
 * Every entry point of the library checks its input against one of these limits before it does any work that grows
 * with the size of that input, so a hostile argument, version string, identifier or text can neither exhaust the
 * memory of a process nor keep it busy. An input above a limit is refused, with a violation, an exception or a `null`
 * result as the entry point documents; it is never silently truncated. Text that is only shown, such as a value in a
 * message, a report or a `toString`, is shortened to [MAX_DISPLAY_LENGTH] or to the limit named for it and is then
 * marked as shortened. A few limits bound what Kore produces rather than what it reads; their descriptions say so.
 *
 * The limits are fixed, except the four limits of the command line, which are only the defaults of
 * [ParameterLimits][com.davils.kore.core.arguments.ParameterLimits] and can be set for each schema of
 * parameters. All limits are generous for every legitimate use and are part of the public contract: raising one is a
 * compatible change, lowering one is a breaking change. Each limit is a compile-time constant, so code that reads one
 * keeps the value it was compiled against until it is compiled again.
 *
 * @since 1.0.0
 */
public object Limits {
    /**
     * The most command line arguments one schema of parameters reads, by default.
     *
     * It is the default of [maxArguments][com.davils.kore.core.arguments.ParameterLimits.maxArguments]. A command
     * line with more arguments is refused as a whole before any argument is read.
     *
     * @since 1.0.0
     */
    public const val MAX_ARGUMENTS: Int = 4_096

    /**
     * The longest single argument or value, in UTF-16 code units, that one schema of parameters reads, by default.
     *
     * It is the default of
     * [maxArgumentLength][com.davils.kore.core.arguments.ParameterLimits.maxArgumentLength] and applies to the
     * arguments of the command line as well as to values from the environment. A longer argument or value is refused
     * with a violation.
     *
     * @since 1.0.0
     */
    public const val MAX_ARGUMENT_LENGTH: Int = 65_536

    /**
     * The most values one parameter takes, counted after a value was split at its delimiter, by default.
     *
     * It is the default of
     * [maxValuesPerParameter][com.davils.kore.core.arguments.ParameterLimits.maxValuesPerParameter]. A parameter
     * with more values is refused with a violation.
     *
     * @since 1.0.0
     */
    public const val MAX_VALUES_PER_PARAMETER: Int = 1_024

    /**
     * The most unknown arguments for which a similar declared name is suggested, by default.
     *
     * It is the default of
     * [maxSuggestedArguments][com.davils.kore.core.arguments.ParameterLimits.maxSuggestedArguments] and bounds
     * work Kore does for its messages rather than what it reads: further unknown arguments get no suggestion.
     *
     * @since 1.0.0
     */
    public const val MAX_SUGGESTED_ARGUMENTS: Int = 16

    /**
     * The longest text, in UTF-16 code units, that is read as a number or a duration.
     *
     * Longer text is refused as not being a number or a duration before it is parsed.
     *
     * @since 1.0.0
     */
    public const val MAX_NUMERIC_TEXT_LENGTH: Int = 64

    /**
     * The longest text, in UTF-16 code units, that [parseDecimalOrNull] reads as a decimal number.
     *
     * Longer text is refused before it is parsed. It bounds the decimal parsing that Kore shares with the libraries
     * of Davils; the values of the command line keep to [MAX_NUMERIC_TEXT_LENGTH].
     *
     * @since 2.1.0
     */
    public const val MAX_DECIMAL_TEXT_LENGTH: Int = 1_024

    /**
     * The longest text, in UTF-16 code units, that is read as a semantic version, and the longest a version may
     * become.
     *
     * Longer text is refused before it is parsed, and so is a version built or changed in code whose text would be
     * longer.
     *
     * @since 1.0.0
     */
    public const val MAX_VERSION_LENGTH: Int = 256

    /**
     * The longest text, in UTF-16 code units, that is read as a range of semantic versions.
     *
     * Longer text, blank text included, is refused before it is parsed, and so is text whose canonical form would be
     * longer. A range built in code, such as by a union, is not bound by it, so its text can be too long to be read
     * back.
     *
     * @since 1.0.0
     */
    public const val MAX_RANGE_LENGTH: Int = 1_024

    /**
     * The most comparators, alternatives included, that one range of semantic versions consists of.
     *
     * Each comparator of a comparator set, each hyphen range and each interval of the interval notation counts once,
     * across all alternatives of the range. A range with more is refused, and so is text whose canonical form would
     * have more intervals.
     *
     * @since 1.0.0
     */
    public const val MAX_RANGE_COMPARATORS: Int = 64

    /**
     * The length of the canonical text of a UUID, the only length that is read as one.
     *
     * Text of any other length is refused before it is parsed.
     *
     * @since 1.0.0
     */
    public const val MAX_UUID_TEXT_LENGTH: Int = 36

    /**
     * The most violations one validation collects; every further violation is only counted.
     *
     * It bounds what a validation keeps rather than what it reads: a validation that finds more ends its report with
     * one [TOO_MANY][com.davils.kore.core.validation.violation.ViolationCode.TOO_MANY] error that names how many
     * it left out, so its report holds at most one violation more than this limit.
     *
     * @since 1.0.0
     */
    public const val MAX_VIOLATIONS: Int = 1_000

    /**
     * The most violations of one severity a rendered report lists before it names how many it left out.
     *
     * It bounds only the text of a report; the report itself keeps every collected violation.
     *
     * @since 1.0.0
     */
    public const val MAX_RENDERED_VIOLATIONS: Int = 50

    /**
     * The deepest path, in segments, a validation descends to before it reports the nesting itself as a violation.
     *
     * A nested value below this depth is not validated; a
     * [TOO_DEEP][com.davils.kore.core.validation.violation.ViolationCode.TOO_DEEP] error at the deepest validated
     * path reports it instead, so a deeply nested or cyclic value cannot exhaust the stack.
     *
     * @since 1.0.0
     */
    public const val MAX_PATH_DEPTH: Int = 64

    /**
     * The longest value, in UTF-16 code units, that is shown in a message, a report or a `toString`, and the longest
     * violation code.
     *
     * A longer value is shortened after it was escaped and marked with the number of characters left out; a longer
     * violation code is refused.
     *
     * @since 1.0.0
     */
    public const val MAX_DISPLAY_LENGTH: Int = 256

    /**
     * The longest message, in UTF-16 code units, that a rendered report or the text of a violation shows for one
     * violation, and the longest description of a parameter that a usage text shows.
     *
     * A longer message or description is shortened after it was escaped and marked with the number of characters left
     * out; the violation itself keeps the whole message.
     *
     * @since 1.0.0
     */
    public const val MAX_MESSAGE_LENGTH: Int = 1_024

    /**
     * The longest id of a principal, in UTF-16 code units.
     *
     * A principal with a longer id fails its validation.
     *
     * @since 1.0.0
     */
    public const val MAX_PRINCIPAL_ID_LENGTH: Int = 256

    /**
     * The longest role, tenant or display name of a principal, in UTF-16 code units.
     *
     * A principal with a longer one fails its validation.
     *
     * @since 1.0.0
     */
    public const val MAX_PRINCIPAL_TEXT_LENGTH: Int = 256

    /**
     * The most roles one principal holds.
     *
     * A principal with more fails its validation.
     *
     * @since 1.0.0
     */
    public const val MAX_ROLES: Int = 256

    /**
     * The most attributes one principal carries.
     *
     * Adding an attribute beyond it is refused, by the builder of a principal with a violation and by the attributes
     * themselves with an [IllegalArgumentException].
     *
     * @since 1.0.0
     */
    public const val MAX_ATTRIBUTES: Int = 64

    /**
     * The longest name of an attribute of a principal, in UTF-16 code units.
     *
     * A principal with a longer one fails its validation.
     *
     * @since 1.0.0
     */
    public const val MAX_ATTRIBUTE_NAME_LENGTH: Int = 256

    /**
     * The longest value of an attribute of a principal, in UTF-16 code units.
     *
     * A principal with a longer one fails its validation.
     *
     * @since 1.0.0
     */
    public const val MAX_ATTRIBUTE_VALUE_LENGTH: Int = 4_096

    /**
     * The longest chain of principals one principal acts on behalf of.
     *
     * A principal with a longer chain fails its validation.
     *
     * @since 1.0.0
     */
    public const val MAX_DELEGATION_CHAIN: Int = 8

    /**
     * The widest window, in UTF-16 code units, that a snippet of text shows around its highlight.
     *
     * It bounds what Kore produces: a snippet asked for with a wider window gets this width instead.
     *
     * @since 1.0.0
     */
    public const val MAX_SNIPPET_WIDTH: Int = 1_024

    /**
     * The longest text, in UTF-16 code units, whose lines are indexed.
     *
     * A longer text is refused with an [IllegalArgumentException] before it is indexed.
     *
     * @since 1.0.0
     */
    public const val MAX_LINE_MAP_LENGTH: Int = 64 * 1_024 * 1_024

    /**
     * The longest text, in UTF-16 code units, between which an edit distance is measured.
     *
     * [com.davils.kore.core.text.editDistance] refuses a longer text with an [IllegalArgumentException] before it
     * measures anything, and [com.davils.kore.core.text.closestMatch] never suggests a longer candidate. Measuring
     * grows with the product of both lengths, so the limit bounds the work of a single comparison.
     *
     * @since 2.1.0
     */
    public const val MAX_EDIT_DISTANCE_LENGTH: Int = 256

    /**
     * The longest text, in UTF-16 code units, for which close matches are suggested.
     *
     * [com.davils.kore.core.text.closestMatch] and [com.davils.kore.core.text.closeMatches] suggest nothing for a
     * longer text and measure no candidate for it.
     *
     * @since 2.1.0
     */
    public const val MAX_SUGGESTION_TOKEN_LENGTH: Int = 64

    /**
     * The longest name, in characters, that a [com.davils.kore.core.naming.NameRule] accepts, and the default of
     * every rule.
     *
     * A longer text is refused before a single character of it is checked. The limit equals [MAX_DISPLAY_LENGTH], so
     * a valid name is always shown in full.
     *
     * @since 2.1.0
     */
    public const val MAX_NAME_LENGTH: Int = 256

    /**
     * The longest text, in UTF-16 code units, that is read as a data size.
     *
     * [com.davils.kore.core.size.DataSize.parse] refuses a longer text before it reads it.
     *
     * @since 2.1.0
     */
    public const val MAX_DATA_SIZE_TEXT_LENGTH: Int = 32

    /**
     * The longest text, in UTF-16 code units, that is split into words or written in another case style.
     *
     * [com.davils.kore.core.naming.words] refuses a longer text with an [IllegalArgumentException] before it reads it.
     *
     * @since 2.1.0
     */
    public const val MAX_WORDS_TEXT_LENGTH: Int = 1_024

    /**
     * The most bytes that are decoded into text.
     *
     * [com.davils.kore.core.text.encoding.decodeText] refuses more bytes with a failure before it reads any. The limit
     * equals [MAX_LINE_MAP_LENGTH], so decoded text can always be indexed.
     *
     * @since 2.1.0
     */
    public const val MAX_DECODED_TEXT_BYTES: Int = 64 * 1_024 * 1_024
}
