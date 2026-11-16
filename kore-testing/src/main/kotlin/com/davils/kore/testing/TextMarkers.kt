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

import com.davils.kore.core.text.KoreTextRange
import com.davils.kore.testing.KoreMarkedText.Companion.CARET
import com.davils.kore.testing.KoreMarkedText.Companion.RANGE_END
import com.davils.kore.testing.KoreMarkedText.Companion.RANGE_START

private val markers: List<String> = listOf(CARET, RANGE_START, RANGE_END)

internal class TextMarkers(private val marked: String) {
    private val text = StringBuilder()
    private val carets = ArrayList<Int>()
    private val ranges = ArrayList<KoreTextRange>()
    private val openRanges = ArrayDeque<Int>()

    fun read(): KoreMarkedText {
        var index = 0
        while (index < marked.length) {
            index = step(index)
        }
        check(openRanges.isEmpty()) { "Every $RANGE_START needs a $RANGE_END, but ${openRanges.size} stayed open" }
        check(markers.none { marker -> marker in text }) {
            "Removing a marker joined the text around it into another marker; build such a text with the constructor"
        }
        return KoreMarkedText(text = text.toString(), carets = carets, ranges = ranges.sorted())
    }

    private fun step(index: Int): Int {
        val marker = markers.firstOrNull { candidate -> marked.startsWith(candidate, index) }
        if (marker == null) {
            text.append(marked[index])
            return index + 1
        }
        apply(marker)
        return index + marker.length
    }

    private fun apply(marker: String) {
        val offset = text.length
        when (marker) {
            CARET -> carets.add(offset)
            RANGE_START -> openRanges.addLast(offset)
            RANGE_END -> ranges.add(KoreTextRange(start = closeRange(), end = offset))
        }
    }

    private fun closeRange(): Int {
        check(openRanges.isNotEmpty()) { "A $RANGE_END needs a $RANGE_START before it" }
        return openRanges.removeLast()
    }
}
