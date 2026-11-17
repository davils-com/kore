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

import com.davils.kore.core.arguments.declaration.ParameterDraft
import com.davils.kore.core.arguments.report.ParameterViolation
import com.davils.kore.core.arguments.report.ParametersNotLoadedException
import com.davils.kore.core.arguments.report.renderUsage
import com.davils.kore.core.arguments.report.renderValues
import com.davils.kore.core.arguments.resolution.LoadGate
import com.davils.kore.core.arguments.resolution.LoadedArguments
import com.davils.kore.core.arguments.resolution.ParameterRegistry
import com.davils.kore.core.arguments.resolution.ParameterSlot
import com.davils.kore.core.arguments.resolution.SchemaOptions
import com.davils.kore.core.arguments.resolution.loadArguments
import com.davils.kore.core.arguments.resolution.once
import com.davils.kore.core.validation.violation.Severity
import kotlin.reflect.KProperty

/**
 * A schema of the parameters a program reads from its command line, its environment and optional value sources.
 *
 * A program extends this class and declares each parameter as a delegated property, with the functions inherited from
 * [ParameterDeclarations]:
 *
 * ```kotlin
 * class ServerParameters : Parameters(environmentPrefix = "APP") {
 *     val port: Int by defaulted("--port", "-p", type = Converters.int, default = 8080)
 *     val token: String by required("--token") { sensitive() }
 *     val verbose: Boolean by flag("--verbose", "-v")
 * }
 *
 * val parameters = ServerParameters()
 * parameters.load(args)
 * ```
 *
 * A schema is loaded exactly once. [load] reads every value, converts and validates it, and either assigns all
 * parameters or assigns none and throws a [com.davils.kore.core.arguments.report.ParameterException] that names
 * every problem at once. Reading a parameter, [rest], [unknown], [unknownNames], [warnings] or [describe] before a
 * successful load throws [ParametersNotLoadedException]. A second call of [load], also from another thread while
 * the first is still running, throws [IllegalStateException]; a load that failed with an exception leaves the schema
 * unloaded, so it may be loaded again. Once loaded, the schema is effectively immutable and may be read from any
 * thread.
 *
 * A value is taken from the first place that has one: the command line, then the environment variable of the parameter,
 * then each [ValueSource] in the order given, then the declared default. An environment variable set to the empty
 * string counts as not set.
 *
 * The command line grammar is strict. A long name is written `--name`, `--name=value` or `--name value`; a short name
 * `-n`, `-nvalue`, `-n=value` or `-n value`; short flags may be bundled as `-abc`. A following argument is taken as the
 * value only when it does not start with a dash, is a single dash, or is a plain ASCII number such as `-5` or `-2.5`.
 * The first `--` ends the parameters; everything after it is handed over unread as [rest]. A positional argument before
 * `--` is reported as a stray value, and an unknown parameter is an error unless the schema was created with
 * [UnknownArgumentPolicy.Collect].
 *
 * Every input is treated as hostile and checked against the [ParameterLimits] of the schema before it is read: too
 * many arguments, an argument or value that is too long, or a parameter with too many values is refused with a
 * violation, never truncated. Converters, validation rules and value sources are foreign code: whatever they throw is
 * reported as a violation, an [Error] such as an [AssertionError] or a [NotImplementedError] included, and for a
 * sensitive parameter it is kept only as a
 * [com.davils.kore.core.arguments.report.RedactedException] or not at all. Only an [InterruptedException], which
 * is rethrown with the interrupt flag of the thread restored, a [kotlin.coroutines.cancellation.CancellationException]
 * and a [VirtualMachineError], such as an [OutOfMemoryError] or a [StackOverflowError], escape the load unchanged.
 *
 * Every text a schema renders, such as [usage], [describe], the message of a failed load and
 * [ParameterViolation.toString], is escaped, so an attacker who controls an argument cannot forge lines or control
 * the terminal. The value of a parameter declared `sensitive()` is shown only as `****`, which reveals neither its
 * length nor whether there is more than one, and user information in a URI is masked wherever a value is shown. Kore
 * never decides from the name of a parameter, such as `--password`, that it is sensitive; only `sensitive()` makes it
 * so.
 *
 * A subclass declares parameter groups in its initializer with two protected functions. `exclusive(vararg parameters)`
 * refuses a load in which more than one of the given properties received a value, and
 * `requires(parameter, vararg required)` refuses a load in which `parameter` received a value but one of `required` did
 * not; a default does not count as a value. Both throw [IllegalStateException] once loading has started, and a group
 * that names a property that is not a parameter of this schema makes [load] throw [IllegalArgumentException].
 *
 * @param environmentPrefix the prefix from which the environment variable of a parameter is derived from its first long
 * name, such as `APP_DATA_DIR` for `--data-dir` with the prefix `APP`; a parameter with short names only gets no
 * variable, and with `null` only the variables named with `env()` are read. A derived name must be a valid variable
 * name of ASCII letters, digits and underscores, or declaring the parameter throws [IllegalArgumentException].
 * @param unknownArguments what happens to an argument that names no declared parameter;
 * [UnknownArgumentPolicy.Reject] refuses the load.
 * @param limits the bounds every load enforces; the defaults come from [com.davils.kore.core.Limits].
 * @param sensitiveOnCommandLine the severity of the violation reported when a sensitive parameter is given on the
 * command line, where the process list and the shell history show it; [Severity.Error] refuses such a load.
 * @since 1.0.0
 */
public abstract class Parameters(
    environmentPrefix: String? = null,
    unknownArguments: UnknownArgumentPolicy = UnknownArgumentPolicy.Reject,
    limits: ParameterLimits = ParameterLimits(),
    sensitiveOnCommandLine: Severity = Severity.Warning,
) : ParameterDeclarations() {
    private val options: SchemaOptions = SchemaOptions(
        unknownArguments = unknownArguments,
        limits = limits,
        sensitiveOnCommandLine = sensitiveOnCommandLine,
    )
    private val registry: ParameterRegistry = ParameterRegistry(
        schemaName = this::class.simpleName.orEmpty(),
        environmentPrefix = environmentPrefix,
        limits = limits,
    )
    private val gate: LoadGate = LoadGate()

    /**
     * Whether a load of this schema has succeeded, so that its parameters can be read.
     *
     * @since 1.0.0
     */
    public val isLoaded: Boolean get() = gate.loaded != null

    /**
     * The arguments after the first `--`, in their order and exactly as given.
     *
     * They are neither read, checked against the length limit, escaped nor masked: whatever the caller does with them,
     * it treats them as untrusted input. The list is empty when there was no `--`, and it cannot be changed.
     *
     * @throws ParametersNotLoadedException if the schema has not been loaded.
     * @since 1.0.0
     */
    public val rest: List<String> get() = requireLoaded("rest").rest

    /**
     * The arguments before `--` that named no declared parameter or belonged to none, exactly as given.
     *
     * Only a schema with [UnknownArgumentPolicy.Collect] collects them; with [UnknownArgumentPolicy.Reject] an
     * unknown argument refuses the load, so the list is always empty. Each entry is the whole raw token, including any
     * value written into it, such as `--password=secret`: it is not masked, escaped or shortened, and Kore never
     * renders it. Prefer [unknownNames] whenever the names are enough, for example to log what was ignored.
     *
     * @throws ParametersNotLoadedException if the schema has not been loaded.
     * @since 1.0.0
     */
    public val unknown: List<String> get() = requireLoaded("unknown").unknown

    /**
     * The names of the unknown parameters that [unknown] collected, without the values written into them.
     *
     * An unknown long argument contributes the part before its `=`, such as `--password` for `--password=secret`, and a
     * bundle of short flags contributes its first undeclared letter, such as `-x`; a stray value contributes nothing.
     * The names come from the command line and are not escaped. The list is empty unless the schema uses
     * [UnknownArgumentPolicy.Collect].
     *
     * @throws ParametersNotLoadedException if the schema has not been loaded.
     * @since 1.0.0
     */
    public val unknownNames: List<String> get() = requireLoaded("unknownNames").unknownNames

    /**
     * The violations of the successful load, which are all warnings, such as the use of a deprecated parameter, a
     * sensitive value given on the command line or a warning of a validation rule.
     *
     * Their messages may contain text from the input; their [ParameterViolation.toString] escapes it.
     *
     * @throws ParametersNotLoadedException if the schema has not been loaded.
     * @since 1.0.0
     */
    public val warnings: List<ParameterViolation> get() = requireLoaded("warnings").warnings

    /**
     * Loads the parameters from [argv] and the environment of the process.
     *
     * This is the entry point for the arguments of a `main` function; it behaves exactly like the [load] that takes a
     * list and reads [System.getenv].
     *
     * @param argv the command line arguments, without the name of the program.
     * @throws com.davils.kore.core.arguments.report.ParameterException if any parameter is missing, unknown,
     * malformed, too long or invalid; no parameter is assigned then.
     * @throws IllegalStateException if the schema was already loaded or is being loaded.
     * @throws IllegalArgumentException if a parameter group names a property that is not a parameter of this schema.
     * @throws InterruptedException if a converter or a validation rule was interrupted.
     * @throws VirtualMachineError if a converter or a validation rule threw one, such as an [OutOfMemoryError].
     * @since 1.0.0
     */
    public fun load(argv: Array<out String>) {
        load(argv.toList())
    }

    /**
     * Loads the parameters from [argv] and [environment], and assigns all of them or none.
     *
     * The arguments are checked against the limits of the schema first; then they are read, every value is converted
     * and validated, and the parameter groups are checked. Every problem is collected, and if any of them is an error
     * the load throws one exception that names all of them and assigns nothing. Otherwise every parameter is assigned
     * and the warnings are kept in [warnings]. A load that throws leaves the schema unloaded, so it may be loaded
     * again.
     *
     * @param argv the command line arguments, without the name of the program.
     * @param environment the environment variables to read; an entry with an empty value counts as not set.
     * @throws com.davils.kore.core.arguments.report.ParameterException if any parameter is missing, unknown,
     * malformed, too long or invalid; no parameter is assigned then.
     * @throws IllegalStateException if the schema was already loaded or is being loaded.
     * @throws IllegalArgumentException if a parameter group names a property that is not a parameter of this schema.
     * @throws InterruptedException if a converter or a validation rule was interrupted.
     * @throws VirtualMachineError if a converter or a validation rule threw one, such as an [OutOfMemoryError].
     * @since 1.0.0
     */
    public fun load(argv: List<String>, environment: Map<String, String> = System.getenv()) {
        gate.once {
            loadArguments(registry = registry, options = options, argv = argv, environment = environment)
        }
    }

    /**
     * Loads the parameters from [argv], [environment] and [sources], and assigns all of them or none.
     *
     * It behaves like the [load] without sources, and in addition asks every source, in the order given, for each
     * parameter that has no value from the command line, the environment or an earlier source. The first value a source
     * returns wins. The sources are asked one after the other in the calling coroutine, and Kore applies no timeout: a
     * caller that cannot wait indefinitely wraps the call in `withTimeout`, and the cancellation is rethrown unchanged.
     * A value from a source is untrusted input like any other and is checked against the limits of the schema,
     * converted and validated. The [ValueSource.name] of a source is read once, before the source is asked for
     * any value. A source that throws, from [ValueSource.read] or from its name, is reported as
     * [ParameterViolation.SourceUnavailable] for each parameter it was to be asked for, which refuses the load; a
     * source whose name throws is not asked at all and is reported as `source` followed by its position in [sources],
     * counted from 1.
     *
     * @param argv the command line arguments, without the name of the program.
     * @param sources the sources asked for the values the command line and the environment do not give.
     * @param environment the environment variables to read; an entry with an empty value counts as not set.
     * @throws com.davils.kore.core.arguments.report.ParameterException if any parameter is missing, unknown,
     * malformed, too long or invalid, or a source failed; no parameter is assigned then.
     * @throws IllegalStateException if the schema was already loaded or is being loaded.
     * @throws IllegalArgumentException if a parameter group names a property that is not a parameter of this schema.
     * @throws InterruptedException if a converter, a validation rule or a source was interrupted.
     * @throws kotlin.coroutines.cancellation.CancellationException if the calling coroutine was cancelled.
     * @throws VirtualMachineError if a converter, a validation rule or a source threw one, such as an
     * [OutOfMemoryError].
     * @since 1.0.0
     */
    public suspend fun load(
        argv: List<String>,
        sources: List<ValueSource>,
        environment: Map<String, String> = System.getenv(),
    ) {
        gate.once {
            loadArguments(
                registry = registry,
                options = options,
                argv = argv,
                sources = sources,
                environment = environment,
            )
        }
    }

    /**
     * Returns the listing of every declared parameter for a help text, which can be shown before the schema is loaded.
     *
     * Each line names a parameter with its type and adds its description, whether it is required or repeatable, its
     * default, its environment variable and whether it is deprecated. The description, type and variable are escaped,
     * the description is shortened to [com.davils.kore.core.Limits.MAX_MESSAGE_LENGTH], and the default of a
     * sensitive parameter is shown as `****`. A schema without parameters returns a sentence that says so.
     *
     * @return the listing, one line per parameter.
     * @since 1.0.0
     */
    public fun usage(): String = renderUsage(registry.infos)

    /**
     * Returns every parameter with the value it was assigned and where that value came from, such as the command line,
     * an environment variable, a source or the default.
     *
     * The values are escaped and shortened, user information in a URI is masked, and the value of a sensitive parameter
     * is shown as `****`. The result is meant for diagnostics, such as a log line at startup.
     *
     * @return the listing, one line per parameter.
     * @throws ParametersNotLoadedException if the schema has not been loaded.
     * @since 1.0.0
     */
    public fun describe(): String {
        requireLoaded("describe()")
        return renderValues(registry.specs)
    }

    /**
     * Declares that at most one of [parameters] may receive a value in a load; a default does not count as a value.
     *
     * @param parameters the properties of this schema that exclude each other.
     * @throws IllegalStateException if loading has already started.
     * @since 1.0.0
     */
    protected fun exclusive(vararg parameters: KProperty<*>) {
        checkNotStarted("An exclusive group")
        registry.exclusive(parameters.map { it.name })
    }

    /**
     * Declares that [parameter] may only receive a value in a load when each of [required] receives one too; a
     * default does not count as a value.
     *
     * @param parameter the property of this schema that depends on the others.
     * @param required the properties of this schema that must receive a value whenever [parameter] does.
     * @throws IllegalStateException if loading has already started.
     * @since 1.0.0
     */
    protected fun requires(parameter: KProperty<*>, vararg required: KProperty<*>) {
        checkNotStarted("A dependency of '${parameter.name}'")
        registry.requires(parameter.name, required.map { it.name })
    }

    internal fun <V> register(draft: ParameterDraft<*, V>, propertyName: String): ParameterSlot<V> {
        checkNotStarted("Parameter '$propertyName'")
        return registry.register(draft, propertyName).slot
    }

    internal fun requireLoaded(subject: String): LoadedArguments = gate.loaded ?: throw notLoaded(subject)

    private fun notLoaded(subject: String): ParametersNotLoadedException = ParametersNotLoadedException(subject)

    private fun checkNotStarted(subject: String) {
        check(!gate.isStarted) { "$subject was declared after load() had already started" }
    }
}
