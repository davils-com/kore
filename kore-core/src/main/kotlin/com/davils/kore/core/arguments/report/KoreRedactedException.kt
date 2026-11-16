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

package com.davils.kore.core.arguments.report

/**
 * Stands in for an exception that a converter or a value source threw for a sensitive parameter.
 *
 * Such an exception may carry the secret in its message, its cause or its stack trace, so it is replaced by this one,
 * which keeps only the class name of the original. It has no cause, no suppressed exceptions and no stack trace, and
 * its message is constant text around that class name. It is created by Kore only, from the exception it replaces,
 * and holds no reference to that exception.
 *
 * @since 1.0.0
 */
public class KoreRedactedException internal constructor(
    original: Throwable,
) : RuntimeException(redactedMessage(original.javaClass.name), null, false, false) {
    /**
     * The fully qualified class name of the exception that was thrown, such as `java.lang.NumberFormatException`.
     *
     * @since 1.0.0
     */
    public val originalType: String = original.javaClass.name
}

internal fun redactedFor(isSensitive: Boolean, failure: Throwable): Throwable {
    if (!isSensitive) return failure
    return KoreRedactedException(failure)
}

private fun redactedMessage(originalType: String): String =
    "$originalType was thrown, and its details are withheld because the parameter is sensitive"
