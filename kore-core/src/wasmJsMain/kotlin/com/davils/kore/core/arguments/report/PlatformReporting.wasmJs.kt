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

private const val UNNAMED_TYPE: String = "Throwable"

internal actual fun maskedByPlatform(value: Any?): String? = null

internal actual fun typeNameOf(failure: Throwable): String {
    val type = failure::class
    return type.qualifiedName ?: type.simpleName ?: UNNAMED_TYPE
}

internal actual fun discardStackTrace(failure: Throwable) = Unit
