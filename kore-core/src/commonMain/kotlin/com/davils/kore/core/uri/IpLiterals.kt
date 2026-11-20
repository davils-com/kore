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

import com.davils.kore.core.text.isAsciiDigit

private const val FUTURE_MARKS: String = "vV"
private const val FUTURE_SEPARATOR: Char = '.'
private const val GROUP_SEPARATOR: Char = ':'
private const val COMPRESSION: String = "::"
private const val OCTET_SEPARATOR: Char = '.'
private const val GROUPS: Int = 8
private const val GROUPS_OF_IPV4: Int = 2
private const val MOST_HEX_DIGITS: Int = 4
private const val OCTETS: Int = 4
private const val MOST_OCTET_DIGITS: Int = 3
private const val LARGEST_OCTET: Int = 255
private const val INVALID: Int = -1

internal fun isIpLiteral(literal: String): Boolean {
    val isFuture = literal.isNotEmpty() && literal.first() in FUTURE_MARKS
    if (isFuture) return isIpFuture(literal)
    return isIpv6(literal)
}

private fun isIpFuture(literal: String): Boolean {
    val separator = literal.indexOf(FUTURE_SEPARATOR)
    val version = literal.substring(startIndex = 1, endIndex = separator.coerceAtLeast(1))
    val address = literal.substring(startIndex = separator + 1)
    val isVersionWellFormed = separator > 1 && version.all(::isHexDigit)
    return isVersionWellFormed && address.isNotEmpty() && address.all(::isFutureCharacter)
}

private fun isFutureCharacter(character: Char): Boolean =
    isUnreserved(character) || isSubDelimiter(character) || character == GROUP_SEPARATOR

private fun isIpv6(literal: String): Boolean {
    val compression = literal.indexOf(COMPRESSION)
    if (compression < 0) return groupsOf(literal, allowsIpv4 = true) == GROUPS
    val isCompressedOnce = literal.indexOf(COMPRESSION, compression + 1) < 0
    val head = groupsOf(literal.substring(startIndex = 0, endIndex = compression), allowsIpv4 = false)
    val tail = groupsOf(literal.substring(startIndex = compression + COMPRESSION.length), allowsIpv4 = true)
    val areGroupsWellFormed = head != INVALID && tail != INVALID
    return isCompressedOnce && areGroupsWellFormed && head + tail < GROUPS
}

private fun groupsOf(text: String, allowsIpv4: Boolean): Int {
    if (text.isEmpty()) return 0
    val groups = text.split(GROUP_SEPARATOR)
    val last = groups.last()
    val hasIpv4End = allowsIpv4 && OCTET_SEPARATOR in last
    val hexGroups = groups.dropLast(1).takeIf { hasIpv4End } ?: groups
    val isLastWellFormed = !hasIpv4End || isIpv4(last)
    val isWellFormed = isLastWellFormed && hexGroups.all(::isHexGroup)
    val count = hexGroups.size + GROUPS_OF_IPV4.takeIf { hasIpv4End }.orZero()
    return count.takeIf { isWellFormed } ?: INVALID
}

private fun Int?.orZero(): Int = this ?: 0

private fun isHexGroup(group: String): Boolean = group.length in 1..MOST_HEX_DIGITS && group.all(::isHexDigit)

private fun isIpv4(text: String): Boolean {
    val octets = text.split(OCTET_SEPARATOR)
    return octets.size == OCTETS && octets.all(::isDecimalOctet)
}

private fun isDecimalOctet(octet: String): Boolean {
    val isShapeWellFormed = octet.length in 1..MOST_OCTET_DIGITS && octet.all(::isAsciiDigit)
    val hasLeadingZero = octet.length > 1 && octet.first() == '0'
    return isShapeWellFormed && !hasLeadingZero && octet.toInt() <= LARGEST_OCTET
}
