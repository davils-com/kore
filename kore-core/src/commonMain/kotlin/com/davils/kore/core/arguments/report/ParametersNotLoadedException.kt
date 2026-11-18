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
 * Thrown when a parameter or a result of a [com.davils.kore.core.arguments.Parameters] schema is read before the
 * schema was loaded successfully.
 *
 * It signals a mistake in the program, not in its input. The message names the property or function that was read.
 *
 * @since 1.0.0
 */
public class ParametersNotLoadedException internal constructor(
    subject: String,
) : IllegalStateException(notLoadedMessage(subject))

private fun notLoadedMessage(subject: String): String = "$subject was accessed before load() was called on its schema"
