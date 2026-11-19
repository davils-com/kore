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

package com.davils.kore.core.collection

/**
 * Returns every element that occurs more than once in this iterable, each of them once.
 *
 * Elements are compared by [Any.equals] and [Any.hashCode]. The elements come in the order of their second occurrence,
 * so the first element of the result is the first one found twice. The work grows linearly with the number of
 * elements; this iterable is read once and not changed. A caller that checks a collection from input bounds its size
 * first.
 *
 * @param T the type of the elements.
 * @return the elements that occur more than once, as a set that no caller can change; empty if every element is
 * distinct.
 * @since 2.1.0
 */
public fun <T> Iterable<T>.duplicates(): Set<T> = duplicatesBy { element -> element }

/**
 * Returns every key that [key] gives for more than one element of this iterable, each of them once.
 *
 * It behaves like [duplicates], applied to the keys of the elements, such as the names of a list of declarations.
 * [key] is called once for every element, in order.
 *
 * @param T the type of the elements.
 * @param K the type of the keys.
 * @param key gives the key of an element.
 * @return the keys that more than one element has, in the order of their second occurrence, as a set that no caller
 * can change.
 * @since 2.1.0
 */
public inline fun <T, K> Iterable<T>.duplicatesBy(key: (T) -> K): Set<K> {
    val seen = HashSet<K>()
    val repeated = LinkedHashSet<K>()
    for (element in this) {
        val current = key(element)
        if (!seen.add(current)) repeated += current
    }
    return readOnlySetOf(repeated)
}

/**
 * Returns the first element that occurs a second time in this iterable, or `null` if every element is distinct.
 *
 * It stops reading at the second occurrence, so it is cheaper than [duplicates] when only the first one matters. A
 * duplicated `null` element also returns `null`; [duplicates] tells both apart.
 *
 * @param T the type of the elements.
 * @return the first element found twice, or `null`.
 * @since 2.1.0
 */
public fun <T> Iterable<T>.firstDuplicateOrNull(): T? {
    val seen = HashSet<T>()
    return firstOrNull { element -> !seen.add(element) }
}

@PublishedApi
internal fun <K> readOnlySetOf(elements: Set<K>): Set<K> = ReadOnlySet(elements)
