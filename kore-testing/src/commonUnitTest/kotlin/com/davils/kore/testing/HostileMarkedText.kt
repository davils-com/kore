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

package com.davils.kore.testing

import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map

private const val LONGEST_SOUP: Int = 24
private const val ESCAPE: String = "\u001B"
private const val RIGHT_TO_LEFT_OVERRIDE: String = "\u202E"
private const val LINE_SEPARATOR: String = "\u2028"
private const val LONE_HIGH_SURROGATE: String = "\uD800"

internal const val HOSTILE_MARKED_TEXT: String = "$ESCAPE[2J<caret>${RIGHT_TO_LEFT_OVERRIDE}admin\n2026-11-16 INFO ok" +
    "<range>$LINE_SEPARATOR$LONE_HIGH_SURROGATE</range>"

internal fun isPlainAscii(text: String): Boolean = text.all { character -> character in ' '..'~' }

private val markerPieces: List<String> = listOf(
    "<", ">", "/", "car", "et", "ran", "ge", "x", "<caret>", "<range>", "</range>", "<car", "et>", "</ran", "<ran",
)

internal val markerSoup: Arb<String> =
    Arb.list(Arb.element(markerPieces), 0..LONGEST_SOUP).map { pieces -> pieces.joinToString(separator = "") }
