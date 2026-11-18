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
import com.davils.kore.core.arguments.declaration.ParameterSpec
import com.davils.kore.core.arguments.declaration.SHORT_PREFIX
import com.davils.kore.core.arguments.resolution.SchemaOptions

private const val TERMINATOR: String = "--"

internal class ArgumentLexer(specs: List<ParameterSpec<*, *>>, private val options: SchemaOptions) {
    private val matches: Map<String, NameMatch> = indexOf(specs)
    private val longReader: LongTokenReader = LongTokenReader(matches)
    private val shortReader: ShortTokenReader = ShortTokenReader(matches)

    fun lex(argv: List<String>): LexResult {
        val end = lexedCount(argv)
        val tokens = argv.take(end)
        val state = LexState(matches.keys, options)
        var index = 0
        while (index < tokens.size) {
            index = readToken(Cursor(tokens, index), state)
        }
        return state.result(argv.drop(end + 1))
    }

    private fun readToken(cursor: Cursor, state: LexState): Int {
        val token = cursor.token
        val isNamed = token.startsWith(SHORT_PREFIX) && token.length > SHORT_PREFIX.length
        if (!isNamed) {
            state.rejectValue(token, cursor.position)
            return cursor.nextIndex
        }
        return readNamed(cursor, state)
    }

    private fun readNamed(cursor: Cursor, state: LexState): Int {
        if (cursor.token.startsWith(LONG_PREFIX)) return longReader.read(cursor, state)
        return shortReader.read(cursor, state)
    }
}

internal fun lexedCount(argv: List<String>): Int {
    val terminator = argv.indexOf(TERMINATOR)
    return terminator.takeIf { it >= 0 } ?: argv.size
}

private fun indexOf(specs: List<ParameterSpec<*, *>>): Map<String, NameMatch> = buildMap {
    for (spec in specs) {
        spec.info.names.forEach { name -> put(name, NameMatch(spec, isNegated = false)) }
        spec.negatedNames.forEach { name -> put(name, NameMatch(spec, isNegated = true)) }
    }
}
