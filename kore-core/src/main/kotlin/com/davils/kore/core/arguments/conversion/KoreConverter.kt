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

private const val UNNAMED_TYPE: String = "value"

/**
 * Converts the text of an argument, an environment variable or a source into a value of type `T`.
 *
 * The built-in converters are in [KoreConverters]; a lambda is a converter as well, and [KoreConverters.custom] gives
 * one a type name. The text a converter receives from the command line, the environment or a source is untrusted input
 * of at most [com.davils.kore.core.arguments.KoreParameterLimits.maxArgumentLength] UTF-16 code units; a converter
 * refuses what it cannot read by throwing a [KoreConversionException]. Any other exception is contained as well and
 * reported as a conversion failure, except an [InterruptedException], a
 * [kotlin.coroutines.cancellation.CancellationException] or an [Error], which escape the load. A converter is called on
 * the thread that loads the schema; one that is shared between schemas loaded at the same time must be thread-safe.
 *
 * @param T the type of the values this converter returns.
 * @since 1.0.0
 */
public fun interface KoreConverter<out T> {
    /**
     * The name of the type of the values, which the usage lists and conversion failures name, such as `int`; `value`
     * unless the converter names it.
     *
     * @since 1.0.0
     */
    public val typeName: String get() = UNNAMED_TYPE

    /**
     * Converts [raw] into a value.
     *
     * @param raw the text as it was given, untrusted and not trimmed.
     * @return the value the text stands for.
     * @throws KoreConversionException if the text is not a valid value of the type.
     * @since 1.0.0
     */
    public fun convert(raw: String): T
}
