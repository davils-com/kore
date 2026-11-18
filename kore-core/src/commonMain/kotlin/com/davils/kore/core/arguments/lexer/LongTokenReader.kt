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

import com.davils.kore.core.arguments.declaration.LONG_PREFIX

private const val VALUE_SEPARATOR: Char = '='
private const val TRUE_LITERAL: String = "true"
private const val FALSE_LITERAL: String = "false"
private const val NO_NAME_REASON: String = "has no parameter name"
private const val NEGATED_VALUE_REASON: String = "is a negated flag and does not take a value"

internal class LongTokenReader(private val matches: Map<String, NameMatch>) {
    fun read(cursor: Cursor, state: LexState): Int {
        val token = cursor.token
        val name = token.substringBefore(VALUE_SEPARATOR)
        val match = matches[name]
        if (match == null) {
            reject(name, token, state)
            return cursor.nextIndex
        }
        return readMatched(cursor = cursor, match = match, inlineValue = inlineValueOf(token), state = state)
    }

    private fun reject(name: String, token: String, state: LexState) {
        if (name.length <= LONG_PREFIX.length) {
            state.malformed(token = name, reason = NO_NAME_REASON)
            return
        }
        state.reject(name = name, token = token)
    }

    private fun readMatched(cursor: Cursor, match: NameMatch, inlineValue: String?, state: LexState): Int {
        if (match.isNegated && inlineValue != null) {
            state.malformed(token = cursor.token.substringBefore(VALUE_SEPARATOR), reason = NEGATED_VALUE_REASON)
            return cursor.nextIndex
        }
        return consume(cursor = cursor, match = match, inlineValue = inlineValue, state = state)
    }

    private fun consume(cursor: Cursor, match: NameMatch, inlineValue: String?, state: LexState): Int {
        val spec = match.spec
        val value = inlineValue ?: flagLiteral(match)
        if (value == null) {
            return takeFollowingValue(
                cursor = cursor,
                spec = spec,
                token = spec.info.primaryName,
                state = state,
            )
        }
        state.record(spec, value)
        return cursor.nextIndex
    }
}

private fun inlineValueOf(token: String): String? {
    val separator = token.indexOf(VALUE_SEPARATOR)
    if (separator < 0) return null
    return token.substring(separator + 1)
}

private fun flagLiteral(match: NameMatch): String? {
    if (!match.spec.info.isFlag) return null
    return FALSE_LITERAL.takeIf { match.isNegated } ?: TRUE_LITERAL
}
