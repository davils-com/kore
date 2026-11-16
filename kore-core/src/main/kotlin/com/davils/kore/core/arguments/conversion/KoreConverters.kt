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

import com.davils.kore.core.version.KoreSemanticVersion
import com.davils.kore.core.version.KoreSemanticVersionRange
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.time.Duration

private const val HTTPS: String = "https"

/**
 * The built-in converters for the `type` of a parameter.
 *
 * Every converter here, apart from the one that [custom] wraps, is strict, stateless and thread-safe. It reads the text
 * exactly as given, without trimming, without the default locale and without Unicode case folding, and refuses
 * everything else with a [KoreConversionException] whose reason never repeats the input. Converters that take
 * arguments, such as [uri], [duration], [choice], [path] and [file], check those arguments when they are created and
 * throw [IllegalArgumentException] for a contradiction; they also check the declared default of a parameter again when
 * its schema is loaded.
 *
 * @since 1.0.0
 */
public object KoreConverters {
    /**
     * Reads an [Int] written as an optional `-` followed by ASCII digits, such as `8080` or `-1`.
     *
     * Leading zeros are allowed. A leading `+`, whitespace, separators, digits of other scripts and text longer than
     * [com.davils.kore.core.limit.KoreLimits.MAX_NUMERIC_TEXT_LENGTH] are refused before the text is parsed, and a
     * number outside the range of [Int] is refused for its size.
     *
     * @since 1.0.0
     */
    public val int: KoreConverter<Int> = NamedConverter(typeName = "int", reading = ::readInt)

    /**
     * Reads a [Long] written as an optional `-` followed by ASCII digits, such as `9000000000`.
     *
     * It follows the same grammar and limit as [int], and a number outside the range of [Long] is refused for its size.
     *
     * @since 1.0.0
     */
    public val long: KoreConverter<Long> = NamedConverter(typeName = "long", reading = ::readLong)

    /**
     * Reads a finite [Double] in plain decimal notation, such as `0.5`, `-2.5`, `.5` or `1e-3`.
     *
     * The text is an optional `-`, ASCII digits with at most one `.` and at least one digit, and an optional exponent
     * of `e` or `E`, an optional sign and ASCII digits. `NaN`, `Infinity`, hexadecimal floats, the `d` and `f`
     * suffixes, a leading `+`, whitespace and text longer than
     * [com.davils.kore.core.limit.KoreLimits.MAX_NUMERIC_TEXT_LENGTH] are refused, and so is a number that is too large
     * and would be read as infinite, such as `1e999`. A number too small to represent is read as zero.
     *
     * @since 1.0.0
     */
    public val double: KoreConverter<Double> = NamedConverter(typeName = "double", reading = ::readDouble)

    /**
     * Reads a [Boolean] from `true`, `yes`, `on` or `1`, and from `false`, `no`, `off` or `0`.
     *
     * The case of ASCII letters does not matter, so `TRUE` and `Off` are read; no other text is, and letters that only
     * resemble these words under Unicode case folding are refused.
     *
     * @since 1.0.0
     */
    public val boolean: KoreConverter<Boolean> = NamedConverter(typeName = "boolean", reading = ::readBoolean)

    /**
     * Reads a [KoreSemanticVersion], such as `1.4.0` or `2.0.0-rc.1`, with [KoreSemanticVersion.parseOrNull].
     *
     * The grammar and the limits of that function apply. A text that is not a version is refused with a constant reason
     * that does not explain which part of the text is wrong.
     *
     * @since 1.0.0
     */
    public val semanticVersion: KoreConverter<KoreSemanticVersion> = NamedConverter(
        typeName = "version",
        reading = ::readSemanticVersion,
    )

    /**
     * Reads a [KoreSemanticVersionRange], such as `^1.2` or `>=1.0.0 <2.0.0`, with
     * [KoreSemanticVersionRange.parseOrNull].
     *
     * The grammar and the limits of that function apply. A text that is not a range is refused with a constant reason
     * that does not explain which part of the text is wrong.
     *
     * @since 1.0.0
     */
    public val semanticVersionRange: KoreConverter<KoreSemanticVersionRange> = NamedConverter(
        typeName = "range",
        reading = ::readSemanticVersionRange,
    )

    /**
     * Returns a converter that reads a [URI] with [java.net.URI] and checks its scheme, whether it is absolute, whether
     * it names its host where its scheme needs one and whether it carries user information.
     *
     * The defaults accept only absolute `https` URIs without user information. Schemes are compared without regard to
     * the case of ASCII letters. Absolute means that the URI has a scheme, as [URI.isAbsolute] defines it. A URI with
     * the scheme `http`, `https`, `ws`, `wss` or `ftp` is refused, whatever [schemes] allows, unless it names its
     * authority after `//`: browsers and most HTTP clients find a host and user information even in forms such as
     * `https:host`, `https:/user:password@host` or `https:///host`, in which [java.net.URI] finds none. A URI of any
     * other scheme need not name a host, so an opaque URI such as `mailto:team@example.com` is accepted, and a caller
     * that needs a host checks [URI.getHost], which is `null` as well for an authority that is not a valid host name.
     * When [requireAbsolute] is `false`, a relative reference has no scheme and is accepted whatever [schemes] says.
     * User information, such as `user:password@`, is detected in server-based and registry-based authorities and in
     * the text of an opaque URI after `://`; a refusal never repeats it. A malformed URI is refused with the reason of
     * the parser, without the parser's exception. The URI is returned as it was parsed, not normalized.
     *
     * Wherever Kore shows a value that is not sensitive, of this converter or any other, it masks user information
     * as `****`. In a [URI] whose authority [java.net.URI] found, that is everything in the authority before its last
     * `@`. In any other value, it is everything from the start of an authority to the last `@` of the text, where an
     * authority starts after a leading scheme, its `:` and any number of slashes or backslashes, none included, after
     * two leading slashes or backslashes, or otherwise after the first `://`; leading spaces and control characters,
     * and tabs and line breaks within this prefix, are skipped as browsers skip them. So user information is masked in
     * every form a client might read, and so is text that only resembles it, such as the local part of
     * `mailto:team@example.com`.
     *
     * @param schemes the schemes to accept; an empty set accepts every scheme.
     * @param requireAbsolute whether a URI without a scheme is refused.
     * @param allowUserInfo whether a URI with user information is accepted.
     * @return the converter, whose type name is `uri`.
     * @throws IllegalArgumentException if a scheme is not an ASCII letter followed by ASCII letters, digits, `+`, `-`
     * or `.`.
     * @since 1.0.0
     */
    public fun uri(
        schemes: Set<String> = setOf(HTTPS),
        requireAbsolute: Boolean = true,
        allowUserInfo: Boolean = false,
    ): KoreConverter<URI> {
        val rules = UriRules(schemes, isAbsoluteRequired = requireAbsolute, isUserInfoAllowed = allowUserInfo)
        return NamedConverter(typeName = "uri", isCheckingDefaults = true, reading = rules::read)
    }

    /**
     * Returns a converter that reads a finite, non-negative [Duration] between [min] and [max].
     *
     * It reads the formats of [Duration.parseOrNull], such as `30s`, `1h 30m` or the ISO 8601 form `PT1H`. Text that is
     * longer than [com.davils.kore.core.limit.KoreLimits.MAX_NUMERIC_TEXT_LENGTH] or contains anything but printable
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
    public fun duration(min: Duration = Duration.ZERO, max: Duration = Duration.INFINITE): KoreConverter<Duration> {
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
    public fun choice(vararg allowed: String): KoreConverter<String> {
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
    public inline fun <reified E : Enum<E>> enum(): KoreConverter<E> =
        enumConverter(E::class.java.simpleName, enumValues<E>().toList())

    /**
     * Returns a converter that reads a [Path] and checks it against [requirements].
     *
     * The text is first checked lexically, before [Path.of] and before the file system is touched: an empty text and
     * one with a control character are refused, and when the JVM runs on Windows so are network paths
     * (`\\server\share`, with either slash), device paths (`\\.\`, `\\?\` and `\??\`), names that Windows reserves for
     * devices, such as `CON`, `NUL`, `COM1` or `LPT¹`, with or without an extension, alternate data streams and names
     * that end in a dot or a space. On other systems these names are legal and accepted. A text that [Path.of] still
     * refuses is reported as not a valid path. Then the requirements are checked in a fixed order, existence first; a
     * check that cannot decide, for example because the file system denies access, refuses the path.
     *
     * The path is returned as it was given, neither absolute, normalized nor resolved, and a relative path is resolved
     * against the working directory of the process wherever a requirement needs it. Every requirement is checked once,
     * when the schema is loaded, and is advisory: the file system can change before the program uses the path, and the
     * returned path is not the real path that was checked, so a symbolic link or a rename made after the load can move
     * it out of a [KorePathRequirement.Within] base or past [KorePathRequirement.NoSymlinks]. A program that must
     * confine its file access opens files with its own checks at the time of use.
     *
     * @param requirements what the path must satisfy when the schema is loaded.
     * @return the converter, whose type name is `path`.
     * @throws IllegalArgumentException if [requirements] contains both [KorePathRequirement.Directory] and
     * [KorePathRequirement.RegularFile], or more than one [KorePathRequirement.Within].
     * @since 1.0.0
     */
    public fun path(vararg requirements: KorePathRequirement): KoreConverter<Path> =
        pathConverter(requirements.toSet(), FileSystemInspector)

    /**
     * Returns a converter that reads a [File] and checks it against [requirements].
     *
     * It reads and checks the text exactly like [path], with the same lexical rules and the same advisory, load-time
     * requirements, and returns the path as a [File].
     *
     * @param requirements what the file must satisfy when the schema is loaded.
     * @return the converter, whose type name is `file`.
     * @throws IllegalArgumentException if [requirements] contains both [KorePathRequirement.Directory] and
     * [KorePathRequirement.RegularFile], or more than one [KorePathRequirement.Within].
     * @since 1.0.0
     */
    public fun file(vararg requirements: KorePathRequirement): KoreConverter<File> =
        fileConverter(requirements.toSet(), FileSystemInspector)

    /**
     * Returns a converter that reads values with [converter] and names their type [typeName] in the usage and in
     * conversion failures.
     *
     * The declared default of a parameter is not converted again for a custom converter; only the validation rules of
     * the parameter check it.
     *
     * @param T the type of the values the converter returns.
     * @param typeName the name of the type, such as `port`; it is escaped wherever it is shown.
     * @param converter the converter that reads the text; it follows the contract of [KoreConverter].
     * @return the converter with the given type name.
     * @since 1.0.0
     */
    public fun <T> custom(typeName: String, converter: KoreConverter<T>): KoreConverter<T> =
        NamedConverter(typeName = typeName, reading = converter::convert)

    @PublishedApi
    internal fun <E : Enum<E>> enumConverter(typeName: String, constants: List<E>): KoreConverter<E> {
        val choices = AsciiChoices(constants.map { constant -> constant.name to constant })
        return NamedConverter(typeName = typeName, reading = choices::read)
    }
}
