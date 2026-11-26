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

package com.davils.kore.core.random

private const val FRACTION_SHIFT: Int = 11
private const val FRACTION_SCALE: Double = 1.0 / (1L shl 53)

/**
 * Returns a random fraction from zero, inclusive, up to one, exclusive, every one of the 2^53 values equally likely.
 *
 * The fraction is built from the 53 most significant of the 64 bits of one call of [Entropy.nextLong], so it is exact
 * as a [Double] and never reaches one. A failure of the source reaches the caller unchanged; a caller that must not
 * fail runs the call through [com.davils.kore.core.failure.contained] and chooses its own fallback.
 *
 * @return the fraction.
 * @since 2.5.0
 */
public fun Entropy.nextFraction(): Double = (nextLong() ushr FRACTION_SHIFT) * FRACTION_SCALE
