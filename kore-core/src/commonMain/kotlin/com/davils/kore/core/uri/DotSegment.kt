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

internal enum class DotSegment(val prefix: String, val isWholeRest: Boolean, val consumed: Int) {
    LEADING_PARENT(prefix = "../", isWholeRest = false, consumed = 3),
    LEADING_CURRENT(prefix = "./", isWholeRest = false, consumed = 2),
    INNER_CURRENT(prefix = "/./", isWholeRest = false, consumed = 2),
    TRAILING_CURRENT(prefix = "/.", isWholeRest = true, consumed = 2),
    INNER_PARENT(prefix = "/../", isWholeRest = false, consumed = 3),
    TRAILING_PARENT(prefix = "/..", isWholeRest = true, consumed = 3),
    LONE_CURRENT(prefix = ".", isWholeRest = true, consumed = 1),
    LONE_PARENT(prefix = "..", isWholeRest = true, consumed = 2),
    ;

    val isDroppingSegment: Boolean get() = this == INNER_PARENT || this == TRAILING_PARENT

    val isEndingInSeparator: Boolean get() = this == TRAILING_CURRENT || this == TRAILING_PARENT

    fun matches(path: String, at: Int): Boolean {
        val isLengthMatching = !isWholeRest || path.length - at == prefix.length
        return isLengthMatching && path.startsWith(prefix, at)
    }
}
