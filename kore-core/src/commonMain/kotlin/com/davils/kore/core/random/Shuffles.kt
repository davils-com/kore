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

import com.davils.kore.core.immutableList

/**
 * Returns the elements of this collection in an order drawn from [entropy], every order equally likely.
 *
 * It shuffles by the algorithm of Fisher and Yates with [Entropy.nextInt], so it draws one number fewer than there are
 * elements, and leaves this collection as it is. Pass [SecureEntropy] wherever the order must not be predictable, such
 * as a shuffled playlist that must not repeat for everyone in the same way.
 *
 * @param T the type of the elements.
 * @param entropy the source of the order.
 * @return a new, immutable list of the same elements.
 * @since 2.4.0
 */
public fun <T : Any> Iterable<T>.shuffled(entropy: Entropy): List<T> {
    val items = toMutableList()
    for (index in items.lastIndex downTo 1) {
        val other = entropy.nextInt(index + 1)
        val held = items[index]
        items[index] = items[other]
        items[other] = held
    }
    return items.immutableList()
}
