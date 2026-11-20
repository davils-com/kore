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

import com.davils.kore.core.annotation.DavilsInternal

/**
 * A mutable set that lists its elements in the order they first came and finds them by comparison through an
 * [OrderedIndex], never by hash code.
 *
 * It suits sets of input, such as names already seen in a document, where elements an attacker chose to collide must
 * not make every lookup slow. [contains], [add] and [remove] need a number of comparisons that grows with the
 * logarithm of the number of distinct elements the set was ever given. An iterator walks a copy taken when it was
 * created, so the set may change while it is iterated; it cannot remove elements.
 *
 * A removed element keeps its place: when it comes back, it is listed where it first came, not at the end. The set
 * holds every element it was ever given until [clear], removed elements included, so a caller that fills it from input
 * bounds the number of elements first. The order of the elements must be total and consistent, as [OrderedIndex]
 * describes. Equality and the hash code are those of every [Set]: equal to any set with the same elements, in any
 * order.
 *
 * A set is mutable and not safe to use from several threads at once without synchronization of the caller.
 *
 * @param E the type of the elements.
 * @param elements the elements the set starts with, in order; repeated elements are added once.
 * @since 2.2.0
 */
@DavilsInternal
public class OrderedSet<E : Comparable<E>>(elements: Iterable<E> = emptyList()) : AbstractSet<E>() {
    private val members = OrderedMap<E, Unit>()

    init {
        elements.forEach(::add)
    }

    /**
     * The number of elements in this set.
     *
     * @since 2.2.0
     */
    override val size: Int
        get() = members.size

    /**
     * Returns whether [element] is in this set.
     *
     * @param element the element to look for.
     * @return `true` if [element] is in this set, `false` otherwise.
     * @since 2.2.0
     */
    override fun contains(element: E): Boolean = element in members

    /**
     * Returns an iterator over a copy of the elements, in the order they first came.
     *
     * @return an iterator that is not affected by later changes of this set.
     * @since 2.2.0
     */
    override fun iterator(): Iterator<E> = members.keys.iterator()

    /**
     * Adds [element] to this set.
     *
     * @param element the element to add.
     * @return `true` if [element] was not in this set before, `false` otherwise.
     * @since 2.2.0
     */
    public fun add(element: E): Boolean {
        val isNew = element !in members
        members[element] = Unit
        return isNew
    }

    /**
     * Removes [element] from this set.
     *
     * @param element the element to remove.
     * @return `true` if [element] was in this set, `false` otherwise.
     * @since 2.2.0
     */
    public fun remove(element: E): Boolean = members.remove(element) != null

    /**
     * Removes every element, so that the set is empty and the next element it is given comes first.
     *
     * @since 2.2.0
     */
    public fun clear() {
        members.clear()
    }
}
