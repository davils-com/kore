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

import com.davils.kore.core.arguments.conversion.KoreConverter
import com.davils.kore.core.arguments.conversion.stringConverter
import com.davils.kore.core.arguments.declaration.Arity
import com.davils.kore.core.arguments.declaration.KoreFlagConfiguration
import com.davils.kore.core.arguments.declaration.KoreMultipleConfiguration
import com.davils.kore.core.arguments.declaration.KoreValueConfiguration
import com.davils.kore.core.arguments.declaration.flagParameter
import com.davils.kore.core.arguments.declaration.multipleParameter
import com.davils.kore.core.arguments.declaration.valueParameter

/**
 * The functions with which a [KoreParameters] schema declares its parameters; only [KoreParameters] extends it.
 *
 * Each function is protected, returns a [KoreParameter] and is used with `by` on a property of the schema that calls
 * it, and each is documented on its own as well. The kind of function decides the type of the property and what
 * happens when no value is given:
 *
 * - `optional(names, type, configure)` reads a single value into a nullable property, which is `null` when none is
 *   given.
 * - `required(names, type, configure)` reads a single value; a missing value refuses the load.
 * - `defaulted(names, type, default, configure)` reads a single value and falls back to `default`.
 * - `multiple(names, type, default, configure)` reads every occurrence into a list, which is `default` when none is
 *   given; with `split(delimiter)` each value is also split into pieces.
 * - `flag(names, default, configure)` reads a [Boolean] that `--name` sets to `true`; it also accepts an explicit value
 *   such as `--name=false`.
 *
 * Without `type` a value is kept as the text that was given; with one, such as
 * [com.davils.kore.core.arguments.conversion.KoreConverters.int], it is converted, and the conversion refuses what it
 * cannot read. A single-value parameter given more than once refuses the load. The `configure` block runs once, when
 * the function is called, and declares the description, the environment variable, the deprecation, sensitivity and
 * validation rules of the parameter.
 *
 * Every name starts with a dash. A long name is `--` followed by an ASCII letter or digit and then ASCII letters,
 * digits, `-` or `_`, such as `--data-dir`; a short name is a dash and a single ASCII letter or digit, such as `-d`.
 * The first name is the primary name that reports and listings use. A function throws [IllegalArgumentException] when
 * it is given no name or a name outside this grammar, and the delegation throws [IllegalArgumentException] when a name
 * or an environment variable is already used by another parameter of the schema and [IllegalStateException] once
 * loading has started.
 *
 * A `default` is trusted code and is not read from text, but it is checked when the schema is loaded: every validation
 * rule of the parameter runs on it, and the converters of URIs, durations, choices and paths also convert its text form
 * again, so a default that breaks a declared constraint refuses the load. The default of a sensitive parameter is
 * listed as `****`.
 *
 * @since 1.0.0
 */
public abstract class KoreParameterDeclarations internal constructor() {
    /**
     * Declares a parameter that takes at most one value and is `null` when none is given.
     *
     * The value is kept as the text that was given.
     *
     * @param names the names of the parameter, the primary one first, such as `--name` and `-n`.
     * @param configure declares the description, environment variable, deprecation, sensitivity and rules.
     * @return the parameter, to be used with `by` on a nullable property of the schema.
     * @throws IllegalArgumentException if no name is given or a name breaks the grammar of names.
     * @since 1.0.0
     */
    protected fun optional(
        vararg names: String,
        configure: KoreValueConfiguration<String>.() -> Unit = {},
    ): KoreParameter<String?> = valueParameter(
        owner = this,
        names = names,
        type = stringConverter,
        configure = configure,
        arity = Arity.Optional(),
    )

    /**
     * Declares a parameter that takes at most one value, converted by [type], and is `null` when none is given.
     *
     * @param T the type of the converted value.
     * @param names the names of the parameter, the primary one first, such as `--port` and `-p`.
     * @param type the converter that reads the value and refuses what it cannot read.
     * @param configure declares the description, environment variable, deprecation, sensitivity and rules.
     * @return the parameter, to be used with `by` on a nullable property of the schema.
     * @throws IllegalArgumentException if no name is given or a name breaks the grammar of names.
     * @since 1.0.0
     */
    protected fun <T : Any> optional(
        vararg names: String,
        type: KoreConverter<T>,
        configure: KoreValueConfiguration<T>.() -> Unit = {},
    ): KoreParameter<T?> = valueParameter(
        owner = this,
        names = names,
        type = type,
        configure = configure,
        arity = Arity.Optional(),
    )

    /**
     * Declares a parameter that takes exactly one value; a missing value refuses the load.
     *
     * The value is kept as the text that was given.
     *
     * @param names the names of the parameter, the primary one first, such as `--name` and `-n`.
     * @param configure declares the description, environment variable, deprecation, sensitivity and rules.
     * @return the parameter, to be used with `by` on a property of the schema.
     * @throws IllegalArgumentException if no name is given or a name breaks the grammar of names.
     * @since 1.0.0
     */
    protected fun required(
        vararg names: String,
        configure: KoreValueConfiguration<String>.() -> Unit = {},
    ): KoreParameter<String> = valueParameter(
        owner = this,
        names = names,
        type = stringConverter,
        configure = configure,
        arity = Arity.Required(),
    )

    /**
     * Declares a parameter that takes exactly one value, converted by [type]; a missing value refuses the load.
     *
     * @param T the type of the converted value.
     * @param names the names of the parameter, the primary one first, such as `--port` and `-p`.
     * @param type the converter that reads the value and refuses what it cannot read.
     * @param configure declares the description, environment variable, deprecation, sensitivity and rules.
     * @return the parameter, to be used with `by` on a property of the schema.
     * @throws IllegalArgumentException if no name is given or a name breaks the grammar of names.
     * @since 1.0.0
     */
    protected fun <T : Any> required(
        vararg names: String,
        type: KoreConverter<T>,
        configure: KoreValueConfiguration<T>.() -> Unit = {},
    ): KoreParameter<T> = valueParameter(
        owner = this,
        names = names,
        type = type,
        configure = configure,
        arity = Arity.Required(),
    )

    /**
     * Declares a parameter that takes at most one value and falls back to [default] when none is given.
     *
     * The value is kept as the text that was given; the default is checked against every rule when the schema loads.
     *
     * @param names the names of the parameter, the primary one first, such as `--host`.
     * @param default the value used when none is given.
     * @param configure declares the description, environment variable, deprecation, sensitivity and rules.
     * @return the parameter, to be used with `by` on a property of the schema.
     * @throws IllegalArgumentException if no name is given or a name breaks the grammar of names.
     * @since 1.0.0
     */
    protected fun defaulted(
        vararg names: String,
        default: String,
        configure: KoreValueConfiguration<String>.() -> Unit = {},
    ): KoreParameter<String> = valueParameter(
        owner = this,
        names = names,
        type = stringConverter,
        configure = configure,
        arity = Arity.Defaulted(default),
    )

    /**
     * Declares a parameter that takes at most one value, converted by [type], and falls back to [default].
     *
     * The default is checked against every rule when the schema loads, so a default that breaks a constraint refuses
     * the load.
     *
     * @param T the type of the converted value.
     * @param names the names of the parameter, the primary one first, such as `--timeout`.
     * @param type the converter that reads the value and refuses what it cannot read.
     * @param default the value used when none is given.
     * @param configure declares the description, environment variable, deprecation, sensitivity and rules.
     * @return the parameter, to be used with `by` on a property of the schema.
     * @throws IllegalArgumentException if no name is given or a name breaks the grammar of names.
     * @since 1.0.0
     */
    protected fun <T : Any> defaulted(
        vararg names: String,
        type: KoreConverter<T>,
        default: T,
        configure: KoreValueConfiguration<T>.() -> Unit = {},
    ): KoreParameter<T> = valueParameter(
        owner = this,
        names = names,
        type = type,
        configure = configure,
        arity = Arity.Defaulted(default),
    )

    /**
     * Declares a parameter that collects every value given, in order, and is [default] when none is given.
     *
     * With `split(delimiter)` in [configure] each value is also split into pieces; at most
     * [KoreParameterLimits.maxValuesPerParameter] values are accepted.
     *
     * @param names the names of the parameter, the primary one first, such as `--tag`.
     * @param default the values used when none is given.
     * @param configure declares the delimiter, description, environment variable, deprecation, sensitivity and rules.
     * @return the parameter, to be used with `by` on a list property of the schema.
     * @throws IllegalArgumentException if no name is given or a name breaks the grammar of names.
     * @since 1.0.0
     */
    protected fun multiple(
        vararg names: String,
        default: List<String> = emptyList(),
        configure: KoreMultipleConfiguration<String>.() -> Unit = {},
    ): KoreParameter<List<String>> = multipleParameter(
        owner = this,
        names = names,
        type = stringConverter,
        configure = configure,
        defaults = default,
    )

    /**
     * Declares a parameter that collects every value given, each converted by [type], and is [default] when none is
     * given.
     *
     * With `split(delimiter)` in [configure] each value is also split into pieces; at most
     * [KoreParameterLimits.maxValuesPerParameter] values are accepted.
     *
     * @param T the type of a converted value.
     * @param names the names of the parameter, the primary one first, such as `--port`.
     * @param type the converter that reads each value and refuses what it cannot read.
     * @param default the values used when none is given.
     * @param configure declares the delimiter, description, environment variable, deprecation, sensitivity and rules.
     * @return the parameter, to be used with `by` on a list property of the schema.
     * @throws IllegalArgumentException if no name is given or a name breaks the grammar of names.
     * @since 1.0.0
     */
    protected fun <T : Any> multiple(
        vararg names: String,
        type: KoreConverter<T>,
        default: List<T> = emptyList(),
        configure: KoreMultipleConfiguration<T>.() -> Unit = {},
    ): KoreParameter<List<T>> = multipleParameter(
        owner = this,
        names = names,
        type = type,
        configure = configure,
        defaults = default,
    )

    /**
     * Declares a switch that `--name` sets to `true` and that is [default] when it is not given.
     *
     * It also accepts an explicit value such as `--name=false`, and with `negatable()` in [configure] the form
     * `--no-name`.
     *
     * @param names the names of the switch, the primary one first, such as `--verbose` and `-v`.
     * @param default the value used when the switch is not given.
     * @param configure declares the description, negation, environment variable and deprecation.
     * @return the parameter, to be used with `by` on a Boolean property of the schema.
     * @throws IllegalArgumentException if no name is given or a name breaks the grammar of names.
     * @since 1.0.0
     */
    protected fun flag(
        vararg names: String,
        default: Boolean = false,
        configure: KoreFlagConfiguration.() -> Unit = {},
    ): KoreParameter<Boolean> = flagParameter(owner = this, names = names, default = default, configure = configure)
}
