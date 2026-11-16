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

import com.davils.kore.core.arguments.declaration.ParameterKind
import com.davils.kore.core.arguments.declaration.checkedNames
import com.davils.kore.core.arguments.report.ParameterPresentation
import com.davils.kore.core.text.escape.SafeDisplay

/**
 * What a schema declares about one parameter: its names, its environment variable, its type and how it is presented.
 *
 * It is handed to a [KoreValueSource] and carried by every
 * [com.davils.kore.core.arguments.report.KoreParameterViolation] that concerns a parameter. It is immutable and safe to
 * share between threads. Its texts were written by the developer who declared the parameter, or derived from the
 * environment prefix of the schema; apart from [defaultDisplay] none of them is escaped here, so code that shows them
 * escapes them first. Kore creates it, and checks what it holds when it does: the names follow the name grammar, and
 * the default of a sensitive parameter is masked.
 *
 * @property environmentVariable the environment variable the parameter is read from, or `null` when it reads none.
 * @property typeName the name of the type of its values, as the converter calls it, such as `int` or `path`.
 * @since 1.0.0
 */
public class KoreParameterInfo internal constructor(
    names: List<String>,
    public val environmentVariable: String?,
    public val typeName: String,
    kind: ParameterKind,
    presentation: ParameterPresentation,
) {
    /**
     * Every name of the parameter in the order it was declared, such as `--port` and `-p`; each one follows the name
     * grammar of [KoreParameterDeclarations], and the list is never empty and cannot be changed.
     *
     * @since 1.0.0
     */
    public val names: List<String> = checkedNames(names)

    /**
     * Whether the parameter was declared with `required`, so that a load without a value for it fails.
     *
     * @since 1.0.0
     */
    public val isRequired: Boolean = kind == ParameterKind.Required

    /**
     * Whether the parameter was declared `sensitive()`, so that its value is never shown and is masked as `****`.
     *
     * A source that receives a sensitive parameter returns a secret, and must keep it out of its own logs and errors.
     *
     * @since 1.0.0
     */
    public val isSensitive: Boolean = presentation.isSensitive

    /**
     * Whether the parameter was declared with `multiple`, so that it takes every occurrence instead of a single value.
     *
     * @since 1.0.0
     */
    public val isRepeatable: Boolean = kind == ParameterKind.Multiple

    /**
     * Whether the parameter was declared with `flag`, so that its name alone sets it to `true`.
     *
     * @since 1.0.0
     */
    public val isFlag: Boolean = kind == ParameterKind.Flag

    /**
     * The default as the usage lists it, or `null` when there is none to show.
     *
     * It is escaped and shortened, user information in a URI is masked, and the default of a sensitive parameter is
     * `****`. An optional or required parameter, a flag that is off by default and a repeatable parameter without
     * defaults have none.
     *
     * @since 1.0.0
     */
    public val defaultDisplay: String? = presentation.defaultDisplay?.let { shown ->
        SafeDisplay.REDACTED.takeIf { isSensitive } ?: shown
    }

    /**
     * The description declared with `describedAs`, or `null` when there is none.
     *
     * @since 1.0.0
     */
    public val description: String? = presentation.description

    /**
     * The advice declared with `deprecated`, which is empty when the parameter was deprecated without advice, or `null`
     * when it is not deprecated.
     *
     * @since 1.0.0
     */
    public val deprecation: String? = presentation.deprecation

    /**
     * The first of [names], which reports, listings and violation paths use for the parameter.
     *
     * @since 1.0.0
     */
    public val primaryName: String get() = names.first()

    /**
     * Returns the [primaryName], escaped.
     *
     * @return the escaped primary name.
     * @since 1.0.0
     */
    override fun toString(): String = SafeDisplay.text(primaryName)
}
