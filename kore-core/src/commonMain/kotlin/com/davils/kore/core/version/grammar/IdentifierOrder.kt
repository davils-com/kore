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

internal fun comparePreReleases(left: List<String>, right: List<String>): Int {
    if (left.isEmpty() || right.isEmpty()) return left.isEmpty().compareTo(right.isEmpty())
    return compareIdentifierLists(left, right)
}

internal fun compareBuilds(left: List<String>, right: List<String>): Int = compareIdentifierLists(left, right)

private fun compareIdentifierLists(left: List<String>, right: List<String>): Int {
    val shared = minOf(a = left.size, b = right.size)
    for (index in 0 until shared) {
        val decided = compareIdentifier(left = left[index], right = right[index])
        if (decided != 0) return decided
    }
    return left.size.compareTo(right.size)
}

private fun compareIdentifier(left: String, right: String): Int {
    val isLeftNumeric = isNumeric(left)
    val isRightNumeric = isNumeric(right)
    if (isLeftNumeric != isRightNumeric) return isRightNumeric.compareTo(isLeftNumeric)
    return compareSameKind(left = left, right = right, numeric = isLeftNumeric)
}

private fun compareSameKind(left: String, right: String, numeric: Boolean): Int {
    if (numeric) return compareNumerals(left, right)
    return left.compareTo(right)
}

private fun compareNumerals(left: String, right: String): Int {
    val byLength = left.length.compareTo(right.length)
    if (byLength != 0) return byLength
    return left.compareTo(right)
}
