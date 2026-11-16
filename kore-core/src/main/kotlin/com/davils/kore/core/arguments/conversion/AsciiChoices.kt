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

import com.davils.kore.core.text.escape.SafeDisplay

private const val CHOICE_SEPARATOR: String = ", "
private const val NO_CHOICE_MESSAGE: String = "A choice needs at least one value to choose from"

internal class AsciiChoices<out T : Any>(choices: List<Pair<String, T>>) {
    private val byFolded: Map<String, T> = buildMap {
        choices.forEach { (name, value) -> put(asciiLowercase(name), value) }
    }
    private val reason: String = "is not one of ${choices.joinToString(CHOICE_SEPARATOR) { (name, _) -> name }}"

    init {
        require(choices.isNotEmpty()) { NO_CHOICE_MESSAGE }
        val ambiguous = choices.map { (name, _) -> name }.filter { name -> isAmbiguous(name, choices) }
        require(ambiguous.isEmpty()) {
            val shown = ambiguous.joinToString(CHOICE_SEPARATOR) { name -> SafeDisplay.quoted(name) }
            "The values of a choice must differ in more than the case of their ASCII letters, but $shown do not"
        }
    }

    fun read(raw: String): T = byFolded[asciiLowercase(raw)] ?: throw KoreConversionException(reason)
}

private fun isAmbiguous(name: String, choices: List<Pair<String, *>>): Boolean {
    val folded = asciiLowercase(name)
    return choices.count { (other, _) -> asciiLowercase(other) == folded } > 1
}
