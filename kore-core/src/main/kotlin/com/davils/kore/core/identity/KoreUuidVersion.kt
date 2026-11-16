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

private const val RANDOM_NUMBER: Int = 4
private const val TIME_ORDERED_NUMBER: Int = 7

/**
 * The versions of RFC 9562 that Kore reads and mints, each represented by its own subtype of [KoreUuid].
 *
 * Every other version, the nil and the max UUID and every UUID in another variant than the one of RFC 9562 are refused
 * wherever Kore reads an identifier.
 *
 * @property number the version number the identifier carries in its version field, `4` or `7`.
 * @since 1.0.0
 */
public enum class KoreUuidVersion(public val number: Int) {
    /**
     * Version 4, identifiers made of random bits, represented by [KoreUuidV4].
     *
     * @since 1.0.0
     */
    Random(RANDOM_NUMBER),

    /**
     * Version 7, identifiers that start with a timestamp in milliseconds and are ordered by it, represented by
     * [KoreUuidV7].
     *
     * @since 1.0.0
     */
    TimeOrdered(TIME_ORDERED_NUMBER),
}
