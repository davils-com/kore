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

package com.davils.kore.core.version.grammar

import com.davils.kore.core.text.isAsciiDigit
import com.davils.kore.core.text.isAsciiLetter

internal const val ZERO: Char = '0'

internal fun isIdentifier(identifier: String): Boolean =
    identifier.isNotEmpty() && identifier.all(::isIdentifierCharacter)

internal fun isPreReleaseIdentifier(identifier: String): Boolean {
    if (!isIdentifier(identifier)) return false
    return !isNumeric(identifier) || identifier.length == 1 || identifier.first() != ZERO
}

internal fun isNumeric(identifier: String): Boolean = identifier.isNotEmpty() && identifier.all(::isAsciiDigit)

private fun isIdentifierCharacter(character: Char): Boolean =
    isAsciiDigit(character) || isAsciiLetter(character) || character == PRE_RELEASE_SEPARATOR
