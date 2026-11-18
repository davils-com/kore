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

package com.davils.kore.core.arguments.conversion

import com.davils.kore.core.version.SemanticVersion
import com.davils.kore.core.version.SemanticVersionRange
import kotlin.time.Duration

/**
 * The built-in converters for the `type` of a parameter.
 *
 * Every converter here, apart from the one that [custom] wraps, is strict, stateless and thread-safe. It reads the text
 * exactly as given, without trimming, without the default locale and without Unicode case folding, and refuses
 * everything else with a [ConversionException] whose reason never repeats the input. Converters that take
 * arguments, such as [duration] and [choice], check those arguments when they are created and throw
 * [IllegalArgumentException] for a contradiction; they also check the declared default of a parameter again when its
 * schema is loaded. On the JVM, the extensions `uri`, `path` and `file` add converters for `java.net.URI`,
 * `java.nio.file.Path` and `java.io.File` that follow the same contract.
 *
 * @since 1.0.0
 */
public object Converters {
    /**
     * Reads an [Int] written as an optional `-` followed by ASCII digits, such as `8080` or `-1`.
     *
     * Leading zeros are allowed. A leading `+`, whitespace, separators, digits of other scripts and text longer than
     * [com.davils.kore.core.Limits.MAX_NUMERIC_TEXT_LENGTH] are refused before the text is parsed, and a
     * number outside the range of [Int] is refused for its size.
     *
     * @since 1.0.0
     */
    public val int: Converter<Int> = NamedConverter(typeName = "int", reading = ::readInt)

    /**
     * Reads a [Long] written as an optional `-` followed by ASCII digits, such as `9000000000`.
     *
     * It follows the same grammar and limit as [int], and a number outside the range of [Long] is refused for its size.
     *
     * @since 1.0.0
     */
    public val long: Converter<Long> = NamedConverter(typeName = "long", reading = ::readLong)

    /**
     * Reads a finite [Double] in plain decimal notation, such as `0.5`, `-2.5`, `.5` or `1e-3`.
     *
     * The text is an optional `-`, ASCII digits with at most one `.` and at least one digit, and an optional exponent
     * of `e` or `E`, an optional sign and ASCII digits. `NaN`, `Infinity`, hexadecimal floats, the `d` and `f`
     * suffixes, a leading `+`, whitespace and text longer than
     * [com.davils.kore.core.Limits.MAX_NUMERIC_TEXT_LENGTH] are refused, and so is a number that is too large
     * and would be read as infinite, such as `1e999`. A number too small to represent is read as zero.
     *
     * @since 1.0.0
     */
    public val double: Converter<Double> = NamedConverter(typeName = "double", reading = ::readDouble)

    /**
     * Reads a [Boolean] from `true`, `yes`, `on` or `1`, and from `false`, `no`, `off` or `0`.
     *
     * The case of ASCII letters does not matter, so `TRUE` and `Off` are read; no other text is, and letters that only
     * resemble these words under Unicode case folding are refused.
     *
     * @since 1.0.0
     */
    public val boolean: Converter<Boolean> = NamedConverter(typeName = "boolean", reading = ::readBoolean)

    /**
     * Reads a [SemanticVersion], such as `1.4.0` or `2.0.0-rc.1`, with [SemanticVersion.parseOrNull].
     *
     * The grammar and the limits of that function apply. A text that is not a version is refused with a constant reason
     * that does not explain which part of the text is wrong.
     *
     * @since 1.0.0
     */
    public val semanticVersion: Converter<SemanticVersion> = NamedConverter(
        typeName = "version",
        reading = ::readSemanticVersion,
    )

    /**
     * Reads a [SemanticVersionRange], such as `^1.2` or `>=1.0.0 <2.0.0`, with
     * [SemanticVersionRange.parseOrNull].
     *
     * The grammar and the limits of that function apply. A text that is not a range is refused with a constant reason
     * that does not explain which part of the text is wrong.
     *
     * @since 1.0.0
     */
    public val semanticVersionRange: Converter<SemanticVersionRange> = NamedConverter(
        typeName = "range",
        reading = ::readSemanticVersionRange,
    )

    /**
     * Returns a converter that reads a finite, non-negative [Duration] between [min] and [max].
     *
     * It reads the formats of [Duration.parseOrNull], such as `30s`, `1h 30m` or the ISO 8601 form `PT1H`. Text that is
     * longer than [com.davils.kore.core.Limits.MAX_NUMERIC_TEXT_LENGTH] or contains anything but printable
     * ASCII is refused before it is parsed. An infinite duration, a negative one and one outside the bounds are
     * refused, each with its own reason.
     *
     * @param min the shortest duration accepted.
     * @param max the longest duration accepted; [Duration.INFINITE] sets no upper bound, but an infinite duration is
     * still refused.
     * @return the converter, whose type name is `duration`.
     * @throws IllegalArgumentException if [min] is infinite or negative, or [max] is shorter than [min].
     * @since 1.0.0
     */
    public fun duration(min: Duration = Duration.ZERO, max: Duration = Duration.INFINITE): Converter<Duration> {
        require(min.isFinite() && !min.isNegative()) {
            "The shortest accepted duration must be finite and not negative, but was $min"
        }
        require(max >= min) { "The longest accepted duration $max is shorter than the shortest $min" }
        return NamedConverter(typeName = "duration", isCheckingDefaults = true) { raw ->
            readDuration(raw = raw, shortest = min, longest = max)
        }
    }

    /**
     * Returns a converter that reads one of the [allowed] texts and returns it as it was declared.
     *
     * The case of ASCII letters does not matter, so with `debug` allowed the text `DEBUG` reads as `debug`; no other
     * folding applies, so letters that only match under Unicode or locale case rules are refused. The reason of a
     * refusal lists every allowed value.
     *
     * @param allowed the values to choose from.
     * @return the converter, whose type name is `choice`.
     * @throws IllegalArgumentException if [allowed] is empty, or if two of its values differ only in the case of ASCII
     * letters.
     * @since 1.0.0
     */
    public fun choice(vararg allowed: String): Converter<String> {
        val choices = AsciiChoices(allowed.map { value -> value to value })
        return NamedConverter(typeName = "choice", isCheckingDefaults = true, reading = choices::read)
    }

    /**
     * Returns a converter that reads a constant of the enum class [E] by its name.
     *
     * The case of ASCII letters does not matter, so `red` reads as `RED`; no other folding applies. The reason of a
     * refusal lists the names of every constant.
     *
     * @param E the enum class whose constants are read.
     * @return the converter, whose type name is the simple name of [E].
     * @throws IllegalArgumentException if [E] has no constants, or if two of its constants differ only in the case of
     * ASCII letters.
     * @since 1.0.0
     */
    public inline fun <reified E : Enum<E>> enum(): Converter<E> =
        enumConverter(E::class.simpleName.orEmpty(), enumValues<E>().toList())

    /**
     * Returns a converter that reads values with [converter] and names their type [typeName] in the usage and in
     * conversion failures.
     *
     * The declared default of a parameter is not converted again for a custom converter; only the validation rules of
     * the parameter check it.
     *
     * @param T the type of the values the converter returns.
     * @param typeName the name of the type, such as `port`; it is escaped wherever it is shown.
     * @param converter the converter that reads the text; it follows the contract of [Converter].
     * @return the converter with the given type name.
     * @since 1.0.0
     */
    public fun <T> custom(typeName: String, converter: Converter<T>): Converter<T> =
        NamedConverter(typeName = typeName, reading = converter::convert)

    @PublishedApi
    internal fun <E : Enum<E>> enumConverter(typeName: String, constants: List<E>): Converter<E> {
        val choices = AsciiChoices(constants.map { constant -> constant.name to constant })
        return NamedConverter(typeName = typeName, reading = choices::read)
    }
}
