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

import com.davils.kore.core.text.asciiUppercase
import com.davils.kore.core.text.isAsciiLetter

internal const val INVALID_PATH_REASON: String = "is not a valid path"

private const val EMPTY_REASON: String = "is empty"
private const val CONTROL_REASON: String = "contains a control character"
private const val NETWORK_REASON: String = "is a network path"
private const val DEVICE_REASON: String = "is a device path"
private const val RESERVED_REASON: String = "names a device that Windows reserves"
private const val STREAM_REASON: String = "names an alternate data stream"
private const val TRAILING_REASON: String = "has a name that ends in a dot or a space"

private const val BACKSLASH: Char = '\\'
private const val SLASH: Char = '/'
private const val COLON: Char = ':'
private const val DOT: Char = '.'
private const val SPACE: Char = ' '
private const val UNC_PREFIX: String = "\\\\"
private const val OBJECT_NAMESPACE_PREFIX: String = "\\??\\"
private const val DRIVE_LENGTH: Int = 2

private val deviceMarks: Set<Char> = setOf('.', '?')
private val navigationNames: Set<String> = setOf(".", "..")
private val reservedStems: Set<String> = setOf("CON", "PRN", "AUX", "NUL", "CONIN$", "CONOUT$")
private val numberedDevices: Set<String> = setOf("COM", "LPT")
private val deviceNumbers: Set<String> = buildSet {
    for (digit in '0'..'9') add(digit.toString())
    addAll(listOf("¹", "²", "³"))
}

internal fun pathProblem(raw: String, flavour: PathFlavour): String? {
    if (raw.isEmpty()) return EMPTY_REASON
    return characterProblem(raw) ?: flavourProblem(raw, flavour)
}

private fun characterProblem(raw: String): String? {
    if (raw.any(Char::isISOControl)) return CONTROL_REASON
    return INVALID_PATH_REASON.takeIf { hasLoneSurrogate(raw) }
}

private fun hasLoneSurrogate(raw: String): Boolean {
    val surrogate = Character.SURROGATE.toInt()
    return raw.codePoints().anyMatch { codePoint -> Character.getType(codePoint) == surrogate }
}

private fun flavourProblem(raw: String, flavour: PathFlavour): String? = when (flavour) {
    PathFlavour.Posix -> null
    PathFlavour.Windows -> windowsProblem(raw.replace(oldChar = SLASH, newChar = BACKSLASH))
}

private fun windowsProblem(path: String): String? = prefixProblem(path) ?: componentsProblem(path)

private fun prefixProblem(path: String): String? {
    if (path.startsWith(OBJECT_NAMESPACE_PREFIX)) return DEVICE_REASON
    return uncProblem(path)
}

private fun uncProblem(path: String): String? {
    if (!path.startsWith(UNC_PREFIX)) return null
    val mark = path.getOrNull(UNC_PREFIX.length)
    val isDevice = mark != null && mark in deviceMarks
    return DEVICE_REASON.takeIf { isDevice } ?: NETWORK_REASON
}

private fun componentsProblem(path: String): String? {
    val components = withoutDrive(path).split(BACKSLASH)
    return components.firstNotNullOfOrNull(::componentProblem)
}

private fun withoutDrive(path: String): String {
    val hasDrive = path.length >= DRIVE_LENGTH && isAsciiLetter(path[0]) && path[1] == COLON
    if (!hasDrive) return path
    return path.substring(DRIVE_LENGTH)
}

private fun componentProblem(component: String): String? {
    if (COLON in component) return STREAM_REASON
    return component.takeUnless(::isNavigation)?.let(::nameProblem)
}

private fun isNavigation(component: String): Boolean = component.isEmpty() || component in navigationNames

private fun nameProblem(component: String): String? {
    if (component.endsWith(DOT) || component.endsWith(SPACE)) return TRAILING_REASON
    return RESERVED_REASON.takeIf { isReserved(component) }
}

private fun isReserved(component: String): Boolean {
    val stem = asciiUppercase(component.substringBefore(DOT).trimEnd(SPACE))
    if (stem in reservedStems) return true
    val device = stem.dropLast(1)
    return device in numberedDevices && stem.takeLast(1) in deviceNumbers
}
