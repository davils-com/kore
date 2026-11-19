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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.testing

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * Returns whether [text] can be shown to a person as it is: whether it contains no code point that could forge a line
 * of a log, control a terminal or hide a character.
 *
 * Unsafe are the control, format, line and paragraph separator, private use, unassigned and surrogate code points of
 * Unicode, the default ignorable code points and the variation selectors. The check does not ask the code under test:
 * it keeps its own list of invisible code points and of surrogates, and it reads the Unicode categories of the JVM on
 * the JVM and of the JavaScript engine on Wasm.
 *
 * A test asserts with it that whatever a renderer makes of [HostileFragments] is safe, for example
 * `unsafeCodePointsIn(rendered).shouldBeEmpty()`, which names every unsafe code point when it fails.
 *
 * @param text the text to check.
 * @param allowTabs whether a tab counts as safe, as in a snippet that keeps the tabs of its line.
 * @return `true` if [text] contains no unsafe code point.
 * @since 2.1.0
 */
public fun isSafeToShow(text: String, allowTabs: Boolean = false): Boolean {
    val unsafe = unsafeCodePointsIn(text, allowTabs)
    return unsafe.isEmpty()
}

/**
 * Returns the code points of [text] that [isSafeToShow] refuses, in the order they appear, each written as `U+XXXX`.
 *
 * @param text the text to check.
 * @param allowTabs whether a tab counts as safe, as in a snippet that keeps the tabs of its line.
 * @return the unsafe code points, empty if [text] is safe to show. The list cannot be changed.
 * @since 2.1.0
 */
public fun unsafeCodePointsIn(text: String, allowTabs: Boolean = false): List<String> {
    val unsafe = codePointsOf(text).filter { codePoint -> isUnsafeToShow(codePoint, allowTabs) }
    val notations = unsafe.map { codePoint -> notationOf(codePoint) }
    return notations.immutableList()
}
