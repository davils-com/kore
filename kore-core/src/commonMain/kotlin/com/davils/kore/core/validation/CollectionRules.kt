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

package com.davils.kore.core.validation

import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty

/**
 * Reports an error at [property] if [items] has no element.
 *
 * The violation has the code [ViolationCode.REQUIRED] and the message `must not be empty`.
 *
 * @receiver the validation that collects the violation.
 * @param property the property that holds the collection; the violation is reported at it.
 * @param items the collection to check.
 * @return `true` if [items] passed, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun ValidationScope.notEmpty(property: KProperty<*>, items: Collection<*>): Boolean {
    val isValid = items.isNotEmpty()
    if (!isValid) fail(property = property, message = "must not be empty", code = ViolationCode.REQUIRED)
    return isValid
}

/**
 * Reports an error for every element of [items] whose [key] an earlier element already has, such as two servers with
 * the same name.
 *
 * Each violation is reported at the position of the later element, such as `servers[3]`, with the code
 * [ViolationCode.DUPLICATE] and a message that names the position of the first element with that key, such as
 * `duplicates the entry at index 1`; it never quotes the key. [key] is called once for every element, in order, and
 * the work grows linearly with the number of elements, so bound the size of a list from input first.
 *
 * @receiver the validation that collects the violations.
 * @param T the type of the elements.
 * @param K the type of the keys.
 * @param property the property that holds the list; the violations are reported at its positions.
 * @param items the list to check.
 * @param key gives the key that must be unique, such as the name of an element.
 * @return `true` if every key is unique, `false` if a violation was reported.
 * @since 2.1.0
 */
public fun <T, K> ValidationScope.unique(property: KProperty<*>, items: List<T>, key: (T) -> K): Boolean {
    val firstIndices = HashMap<K, Int>()
    val positions = items.mapIndexed { index, item -> IndexedValue(index, firstIndices.getOrPut(key(item)) { index }) }
    val isUnique = positions.all { (index, first) -> index == first }
    each(property = property, items = positions) { (index, first) ->
        if (index != first) fail(message = "duplicates the entry at index $first", code = ViolationCode.DUPLICATE)
    }
    return isUnique
}
