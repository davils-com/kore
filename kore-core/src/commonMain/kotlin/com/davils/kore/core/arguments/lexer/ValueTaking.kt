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

package com.davils.kore.core.arguments.lexer

import com.davils.kore.core.arguments.conversion.isAsciiDecimal
import com.davils.kore.core.arguments.declaration.ParameterSpec
import com.davils.kore.core.arguments.declaration.SHORT_PREFIX

internal fun takeFollowingValue(cursor: Cursor, spec: ParameterSpec<*, *>, token: String, state: LexState): Int {
    val next = cursor.next
    val isValue = next != null && looksLikeValue(next)
    if (!isValue) return missingValue(cursor = cursor, spec = spec, token = token, state = state)
    state.record(spec, next)
    return cursor.indexAfterValue
}

private fun missingValue(cursor: Cursor, spec: ParameterSpec<*, *>, token: String, state: LexState): Int {
    state.missingValue(spec, token)
    val isSkipped = spec.info.isSensitive && cursor.next != null
    if (isSkipped) return cursor.indexAfterValue
    return cursor.nextIndex
}

internal fun looksLikeValue(token: String): Boolean {
    if (!token.startsWith(SHORT_PREFIX) || token == SHORT_PREFIX) return true
    return isAsciiDecimal(token)
}
