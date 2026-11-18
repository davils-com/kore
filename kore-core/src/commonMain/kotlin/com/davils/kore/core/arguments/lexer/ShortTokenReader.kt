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

import com.davils.kore.core.arguments.declaration.ParameterSpec
import com.davils.kore.core.arguments.declaration.SHORT_PREFIX

private const val VALUE_SEPARATOR: Char = '='
private const val TRUE_LITERAL: String = "true"
private const val SHORT_NAME_LENGTH: Int = 2

internal class ShortTokenReader(matches: Map<String, NameMatch>) {
    private val letters: Map<Char, NameMatch> = lettersOf(matches)

    fun read(cursor: Cursor, state: LexState): Int {
        val token = cursor.token
        val stop = bundleStop(token)
        val isUndeclared = stop < token.length && token[stop] !in letters
        if (isUndeclared) {
            state.rejectBundle(letter = SHORT_PREFIX + token[stop], token = token, position = cursor.position)
            return cursor.nextIndex
        }
        return readCluster(cursor = cursor, stop = stop, state = state)
    }

    private fun bundleStop(token: String): Int {
        var position = SHORT_PREFIX.length
        while (position < token.length && continuesTheBundle(token, position)) position++
        return position
    }

    private fun continuesTheBundle(token: String, position: Int): Boolean {
        val match = letters[token[position]] ?: return false
        return match.spec.info.isFlag && token.getOrNull(position + 1) != VALUE_SEPARATOR
    }

    private fun readCluster(cursor: Cursor, stop: Int, state: LexState): Int {
        val token = cursor.token
        for (position in SHORT_PREFIX.length until stop) state.record(matchAt(token, position).spec, TRUE_LITERAL)
        if (stop == token.length) return cursor.nextIndex
        return readClusterEnd(cursor = cursor, end = stop, state = state)
    }

    private fun readClusterEnd(cursor: Cursor, end: Int, state: LexState): Int {
        val token = cursor.token
        val spec = matchAt(token, end).spec
        val remainder = token.substring(end + 1)
        if (spec.info.isFlag) {
            state.record(spec, remainder.substring(1))
            return cursor.nextIndex
        }
        val name = SHORT_PREFIX + token[end]
        return consumeValue(cursor = cursor, spec = spec, name = name, remainder = remainder, state = state)
    }

    private fun consumeValue(
        cursor: Cursor,
        spec: ParameterSpec<*, *>,
        name: String,
        remainder: String,
        state: LexState,
    ): Int {
        if (remainder.isEmpty()) return takeFollowingValue(cursor = cursor, spec = spec, token = name, state = state)
        state.record(spec, remainder.removePrefix(VALUE_SEPARATOR.toString()))
        return cursor.nextIndex
    }

    private fun matchAt(token: String, position: Int): NameMatch = letters.getValue(token[position])
}

private fun lettersOf(matches: Map<String, NameMatch>): Map<Char, NameMatch> {
    val shortNames = matches.filterKeys { name -> name.length == SHORT_NAME_LENGTH }
    return shortNames.mapKeys { (name, _) -> name.last() }
}
