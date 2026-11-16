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

package com.davils.kore.core.identity

import com.davils.kore.core.limit.KoreLimits
import kotlin.uuid.Uuid

internal const val BYTE_LENGTH: Int = 16
private const val VERSION_SHIFT: Int = 12
private const val VERSION_MASK: Long = 0xF
private const val VARIANT_SHIFT: Int = 62
private const val RFC_VARIANT: Long = 0b10

private val canonicalForm = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

internal fun uuidOf(text: String): Uuid? {
    val isCanonical = text.length == KoreLimits.MAX_UUID_TEXT_LENGTH && canonicalForm.matches(text)
    if (!isCanonical) return null
    return Uuid.parseHexDash(text.lowercase())
}

internal fun uuidOf(bytes: ByteArray): Uuid? {
    if (bytes.size != BYTE_LENGTH) return null
    return Uuid.fromByteArray(bytes)
}

internal fun versionOf(uuid: Uuid): KoreUuidVersion? {
    val (high, low) = uuid.toLongs(::Pair)
    if (low ushr VARIANT_SHIFT != RFC_VARIANT) return null
    val number = (high ushr VERSION_SHIFT and VERSION_MASK).toInt()
    return KoreUuidVersion.entries.firstOrNull { it.number == number }
}

internal fun versioned(uuid: Uuid): KoreUuid? {
    val version = versionOf(uuid) ?: return null
    return when (version) {
        KoreUuidVersion.Random -> KoreUuidV4(uuid)
        KoreUuidVersion.TimeOrdered -> KoreUuidV7(uuid)
    }
}

internal fun refusal(text: String, expected: String): String {
    val expectedLength = KoreLimits.MAX_UUID_TEXT_LENGTH
    if (text.length != expectedLength) return "Not a UUID: ${text.length} characters instead of $expectedLength"
    return "Not a $expected UUID in canonical form"
}

internal fun byteRefusal(bytes: ByteArray, expected: String): String {
    if (bytes.size != BYTE_LENGTH) return "Not a UUID: ${bytes.size} bytes instead of $BYTE_LENGTH"
    return "Not a $expected UUID"
}
