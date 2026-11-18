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

import com.davils.kore.core.identity.Uuid
import com.davils.kore.core.identity.UuidV7
import kotlin.uuid.Uuid as KotlinUuid

internal const val MAX_TIMESTAMP: Long = 0xFFFF_FFFF_FFFFL
private const val TIMESTAMP_SHIFT: Int = 16
private const val VERSION_BITS: Long = 0x7000L
private const val VARIANT_BITS: Long = Long.MIN_VALUE
private const val SEQUENCE_MASK: Long = 0x3FFF_FFFF_FFFF_FFFFL

internal fun sequentialUuidV7(epochMilliseconds: Long, sequence: Long): UuidV7 {
    require(epochMilliseconds in 0..MAX_TIMESTAMP) {
        "A version 7 identifier holds 48 bits of milliseconds, but the timestamp is out of that range"
    }
    val high = epochMilliseconds shl TIMESTAMP_SHIFT or VERSION_BITS
    val low = VARIANT_BITS or (sequence and SEQUENCE_MASK)
    val uuid = Uuid.fromKotlinUuid(KotlinUuid.fromLongs(mostSignificantBits = high, leastSignificantBits = low))
    return uuid as UuidV7
}
