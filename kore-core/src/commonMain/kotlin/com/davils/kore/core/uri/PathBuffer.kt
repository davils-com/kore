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

package com.davils.kore.core.uri

internal class PathBuffer(capacity: Int) {
    private val characters = CharArray(capacity)
    private var length = 0

    fun append(path: String, start: Int, end: Int) {
        for (at in start..<end) {
            characters[length] = path[at]
            length++
        }
    }

    fun appendSeparator() {
        characters[length] = SEGMENT_SEPARATOR
        length++
    }

    fun dropLastSegment() {
        var end = length
        while (end > 0 && characters[end - 1] != SEGMENT_SEPARATOR) end--
        length = (end - 1).coerceAtLeast(0)
    }

    override fun toString(): String = characters.concatToString(startIndex = 0, endIndex = length)
}
