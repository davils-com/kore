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

/**
 * Thrown by a [Converter] when it cannot read a text as a value of its type.
 *
 * The [message] is the reason that a report shows after the quoted value, such as `is not a valid port`, so it reads as
 * the end of a sentence about the value. For a parameter that is not sensitive the reason is shown as it is, escaped;
 * for a sensitive one it is replaced by a constant reason, and the exception itself by a
 * [com.davils.kore.core.arguments.report.RedactedException]. Since the value is shown next to the reason anyway,
 * the reason should be constant text that never repeats the input.
 *
 * @property message why the text is not a value of the type, phrased to follow the quoted value.
 * @param cause the exception that made the conversion fail, if there is one. For a parameter that is not sensitive this
 * exception, cause included, becomes the cause of the violation; for a sensitive one neither is kept.
 * @since 1.0.0
 */
public class ConversionException(
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
