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

import java.io.File
import java.net.URI
import java.nio.file.Path

private const val HTTPS: String = "https"

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
public fun Converters.uri(
    schemes: Set<String> = setOf(HTTPS),
    requireAbsolute: Boolean = true,
    allowUserInfo: Boolean = false,
): Converter<URI> {
    val rules = UriRules(schemes, isAbsoluteRequired = requireAbsolute, isUserInfoAllowed = allowUserInfo)
    return NamedConverter(typeName = "uri", isCheckingDefaults = true, reading = rules::read)
}

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
 * it out of a [PathRequirement.Within] base or past [PathRequirement.NoSymlinks]. A program that must
 * confine its file access opens files with its own checks at the time of use.
 *
 * @param requirements what the path must satisfy when the schema is loaded.
 * @return the converter, whose type name is `path`.
 * @throws IllegalArgumentException if [requirements] contains both [PathRequirement.Directory] and
 * [PathRequirement.RegularFile], or more than one [PathRequirement.Within].
 * @since 1.0.0
 */
public fun Converters.path(vararg requirements: PathRequirement): Converter<Path> =
    pathConverter(requirements.toSet(), FileSystemInspector)

/**
 * Returns a converter that reads a [File] and checks it against [requirements].
 *
 * It reads and checks the text exactly like [Converters.path], with the same lexical rules and the same advisory,
 * load-time requirements, and returns the path as a [File].
 *
 * @param requirements what the file must satisfy when the schema is loaded.
 * @return the converter, whose type name is `file`.
 * @throws IllegalArgumentException if [requirements] contains both [PathRequirement.Directory] and
 * [PathRequirement.RegularFile], or more than one [PathRequirement.Within].
 * @since 1.0.0
 */
public fun Converters.file(vararg requirements: PathRequirement): Converter<File> =
    fileConverter(requirements.toSet(), FileSystemInspector)
