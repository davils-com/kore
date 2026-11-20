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

internal fun removeDotSegments(path: String): String {
    val output = PathBuffer(path.length)
    var at = 0
    while (at < path.length) at = removeNextSegment(path, at, output)
    return output.toString()
}

private fun removeNextSegment(path: String, at: Int, output: PathBuffer): Int {
    val dotSegment = DotSegment.entries.firstOrNull { candidate -> candidate.matches(path, at) }
        ?: return moveSegment(path, at, output)
    if (dotSegment.isDroppingSegment) output.dropLastSegment()
    if (dotSegment.isEndingInSeparator) output.appendSeparator()
    return at + dotSegment.consumed
}

private fun moveSegment(path: String, at: Int, output: PathBuffer): Int {
    var end = at + 1
    while (end < path.length && path[end] != SEGMENT_SEPARATOR) end++
    output.append(path, at, end)
    return end
}
