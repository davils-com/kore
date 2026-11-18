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

import kotlin.uuid.Uuid as KotlinUuid

internal const val V4_TEXT: String = "f81d4fae-7dec-41d0-a765-00a0c91e6bf6"
internal const val V7_TEXT: String = "017f22e2-79b0-7cc3-98c4-dc0c0c07398f"
internal const val V7_EPOCH_MILLISECONDS: Long = 1_645_557_742_000
internal const val V1_TEXT: String = "c232ab00-9414-11ec-b3c8-9f6bdeced846"
internal const val WRONG_VARIANT_TEXT: String = "f81d4fae-7dec-41d0-0765-00a0c91e6bf6"
internal const val NIL_TEXT: String = "00000000-0000-0000-0000-000000000000"

private const val VERSION_CLEAR: Long = 0xF000L.inv()
private const val VERSION_4: Long = 0x4000
private const val VERSION_7: Long = 0x7000
private const val VARIANT_CLEAR: Long = 0x3FFF_FFFF_FFFF_FFFF
private const val RFC_VARIANT: Long = Long.MIN_VALUE
private const val VERSION_SHIFT: Int = 12
private const val VERSION_MASK: Int = 0xF
private const val VARIANT_SHIFT: Int = 62

internal fun v4Of(high: Long, low: Long): Uuid = versionedOf(high, low, VERSION_4)

internal fun v7Of(high: Long, low: Long): Uuid = versionedOf(high, low, VERSION_7)

private fun versionedOf(high: Long, low: Long, version: Long): Uuid {
    val versionedHigh = high and VERSION_CLEAR or version
    val variantLow = low and VARIANT_CLEAR or RFC_VARIANT
    return Uuid.fromKotlinUuid(
        KotlinUuid.fromLongs(mostSignificantBits = versionedHigh, leastSignificantBits = variantLow),
    )
}

internal fun versionBitsOf(high: Long): Int = (high ushr VERSION_SHIFT).toInt() and VERSION_MASK

internal fun variantBitsOf(low: Long): Int = (low ushr VARIANT_SHIFT).toInt()
