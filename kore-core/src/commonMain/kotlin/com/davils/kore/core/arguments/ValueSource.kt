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

/**
 * A place outside the command line and the environment from which a schema reads values, such as a secret store or a
 * configuration service.
 *
 * Sources are passed to the suspending `load` of [Parameters], which asks each of them, in the order given, for
 * every parameter that has no value yet. A source is foreign code: whatever it returns is untrusted input that is
 * checked against the limits of the schema, converted and validated like a command line argument, and whatever it
 * throws, from [read] or from [name], is reported as a
 * [com.davils.kore.core.arguments.report.ParameterViolation.SourceUnavailable] that refuses the load, unless it is
 * an [InterruptedException], a [kotlin.coroutines.cancellation.CancellationException] or a [VirtualMachineError]. A
 * cancellation that [read] throws while the loading coroutine is still active does not come from that coroutine and
 * is reported as unavailable as well, with an [IllegalStateException] as the cause. For a sensitive parameter that
 * violation keeps only the class name of the exception, so a source must not rely on its own exception reaching a
 * log. A source that is shared between schemas loaded at the same time must be safe to call from several coroutines
 * at once.
 *
 * @since 1.0.0
 */
public interface ValueSource {
    /**
     * The name under which values of this source are reported and described, such as `vault`.
     *
     * It is shown in violation paths and in [Parameters.describe], escaped. It is not a secret and should name no
     * credential or address that is one. A load reads it once, before it asks the source for any value; when reading it
     * throws, the source is not asked, and every parameter it was to be asked for is reported as unavailable from
     * `source` followed by the position of the source, counted from 1.
     *
     * @since 1.0.0
     */
    public val name: String

    /**
     * Returns the value of [parameter] from this source, or `null` when the source has none.
     *
     * An empty string is a value, unlike an empty environment variable. The call runs in the coroutine that loads the
     * schema, and Kore applies no timeout to it. A source that cannot answer throws; an [InterruptedException] or a
     * [VirtualMachineError] escapes the load instead of becoming a violation, and so does a
     * [kotlin.coroutines.cancellation.CancellationException] once the loading coroutine is cancelled. A cancellation
     * thrown while that coroutine is still active, such as one from an expired `withTimeout` inside the source, is
     * reported as unavailable with an [IllegalStateException] as the cause, so it cannot end the load silently. When
     * [ParameterInfo.isSensitive] is `true` the returned text is a secret, and the source must keep it out of its own
     * logs and exceptions.
     *
     * @param parameter what the schema declares about the parameter whose value is asked for.
     * @return the value as text, or `null` when this source has no value for the parameter.
     * @since 1.0.0
     */
    public suspend fun read(parameter: ParameterInfo): String?
}
