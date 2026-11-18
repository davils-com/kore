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

import java.util.UUID
import kotlin.uuid.Uuid as KotlinUuid

/**
 * Returns this identifier as a [UUID] of the JDK with the same bits.
 *
 * @receiver the identifier to convert.
 * @return the identifier as an immutable [UUID], whose [UUID.version] is 4 or 7.
 * @since 1.0.0
 */
public fun Uuid.toJavaUuid(): UUID = UUID(mostSignificantBits, leastSignificantBits)

/**
 * Takes over a [UUID] of the JDK as an identifier of version 4 or version 7.
 *
 * @receiver the companion of [Uuid].
 * @param uuid the identifier to take over.
 * @return the identifier with the same bits, a [UuidV4] or a [UuidV7].
 * @throws IllegalArgumentException if [uuid] carries another version or variant, such as a version 1 UUID.
 * @since 1.0.0
 */
public fun Uuid.Companion.fromJavaUuid(uuid: UUID): Uuid {
    val bits = KotlinUuid.fromLongs(
        mostSignificantBits = uuid.mostSignificantBits,
        leastSignificantBits = uuid.leastSignificantBits,
    )
    return fromKotlinUuid(bits)
}
