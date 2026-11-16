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

package com.davils.kore.core.arguments

import com.davils.kore.core.limit.KoreLimits

/**
 * The bounds a [KoreParameters] schema enforces on its input before it reads it.
 *
 * Each bound is checked before the work that grows with the input, and input beyond it is refused with a violation that
 * names the limit, never truncated: too many arguments with
 * [com.davils.kore.core.arguments.report.KoreParameterViolation.TooManyArguments], an argument before `--` that is too
 * long with [com.davils.kore.core.arguments.report.KoreParameterViolation.ArgumentTooLong], an environment or source
 * value that is too long with [com.davils.kore.core.arguments.report.KoreParameterViolation.ValueTooLong], and a
 * parameter with too many values with [com.davils.kore.core.arguments.report.KoreParameterViolation.TooManyValues].
 *
 * The defaults are the limits of [KoreLimits] and suit every ordinary program. A smaller limit is always safe; a larger
 * one is accepted, but it raises the memory and the time a hostile command line, environment or source can cost. Limits
 * are immutable and compared by their values.
 *
 * @property maxArguments the most arguments a load reads, counted over the whole command line including everything
 * after `--`; more refuse the load before any argument is read.
 * @property maxArgumentLength the longest argument before `--`, and the longest value from the environment or a source,
 * in UTF-16 code units; the arguments after `--` are not bounded in length.
 * @property maxValuesPerParameter the most values one parameter takes, counted over all its occurrences after
 * splitting; more refuse the parameter without reading any of them. A parameter given on the command line more often
 * than this is refused as well, even when some of its occurrences are empty and would split into no value, and the
 * command line reader keeps at most one occurrence beyond this limit for each parameter, however often it is given,
 * including within a bundle of short flags such as `-vvv`.
 * @property maxSuggestedArguments the most unknown long arguments for which a similar declared name is searched; `0`
 * searches none.
 * @throws IllegalArgumentException if [maxArguments], [maxArgumentLength] or [maxValuesPerParameter] is not positive,
 * or [maxSuggestedArguments] is negative.
 * @since 1.0.0
 */
public class KoreParameterLimits(
    public val maxArguments: Int = KoreLimits.MAX_ARGUMENTS,
    public val maxArgumentLength: Int = KoreLimits.MAX_ARGUMENT_LENGTH,
    public val maxValuesPerParameter: Int = KoreLimits.MAX_VALUES_PER_PARAMETER,
    public val maxSuggestedArguments: Int = KoreLimits.MAX_SUGGESTED_ARGUMENTS,
) {
    private val identity: List<Int>
        get() = listOf(maxArguments, maxArgumentLength, maxValuesPerParameter, maxSuggestedArguments)

    init {
        require(maxArguments > 0) { "The most arguments a schema reads must be positive, but was $maxArguments" }
        require(maxArgumentLength > 0) {
            "The longest argument a schema reads must be positive, but was $maxArgumentLength"
        }
        require(maxValuesPerParameter > 0) {
            "The most values a parameter takes must be positive, but was $maxValuesPerParameter"
        }
        require(maxSuggestedArguments >= 0) {
            "The most unknown arguments that get a suggestion cannot be negative, but was $maxSuggestedArguments"
        }
    }

    internal fun admitsArguments(count: Int): Boolean = count <= maxArguments

    internal fun admitsLength(text: String): Boolean = text.length <= maxArgumentLength

    internal fun admitsValues(count: Int): Boolean = count <= maxValuesPerParameter

    /**
     * Compares two sets of limits by every one of their four bounds.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is a [KoreParameterLimits] with the same bounds.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is KoreParameterLimits && identity == other.identity

    /**
     * Returns a hash code derived from the four bounds.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = identity.hashCode()

    /**
     * Returns the limits with the name and value of each bound, such as `KoreParameterLimits(maxArguments=4096, ...)`.
     *
     * @return the limits as text.
     * @since 1.0.0
     */
    override fun toString(): String = "KoreParameterLimits(maxArguments=$maxArguments, " +
        "maxArgumentLength=$maxArgumentLength, maxValuesPerParameter=$maxValuesPerParameter, " +
        "maxSuggestedArguments=$maxSuggestedArguments)"
}
