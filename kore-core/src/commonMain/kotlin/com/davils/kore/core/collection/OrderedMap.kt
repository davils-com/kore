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
 * A mutable map from keys to values that lists its entries in the order the keys first came and finds them by
 * comparison through an [OrderedIndex], never by hash code.
 *
 * It suits maps keyed by input, such as the attributes of an element or the keys of a mapping, where keys an attacker
 * chose to collide must not make every lookup slow. [get], [set], [contains], [getOrPut] and [remove] need a number
 * of comparisons that grows with the logarithm of the number of distinct keys the map was ever given; [keys] and
 * [entries] copy the map and grow linearly with that number.
 *
 * A removed key keeps its place: when it comes back, it is listed where it first came, not at the end. The map holds
 * every key it was ever given until [clear], removed keys included, so a caller that fills it from input bounds the
 * number of keys first. The order of the keys must be total and consistent, as [OrderedIndex] describes.
 *
 * A map is mutable and not safe to use from several threads at once without synchronization of the caller.
 *
 * @param K the type of the keys.
 * @param V the type of the values.
 * @since 2.2.0
 */
@DavilsInternal
public class OrderedMap<K : Comparable<K>, V : Any> {
    private var index = OrderedIndex<K>()
    private val ordinalKeys = ArrayList<K>()
    private val ordinalValues = ArrayList<V?>()

    /**
     * The number of keys that have a value.
     *
     * @since 2.2.0
     */
    public var size: Int = 0
        private set

    /**
     * The keys that have a value, in the order they first came, as a list that no caller can change.
     *
     * @since 2.2.0
     */
    public val keys: List<K>
        get() = buildList {
            ordinalKeys.forEachIndexed { ordinal, key -> if (ordinalValues[ordinal] != null) add(key) }
        }

    /**
     * The keys that have a value with their values, in the order the keys first came, as a list that no caller can
     * change.
     *
     * @since 2.2.0
     */
    public val entries: List<Pair<K, V>>
        get() = buildList {
            ordinalKeys.forEachIndexed { ordinal, key -> ordinalValues[ordinal]?.let { value -> add(key to value) } }
        }

    /**
     * Returns the value of [key], or `null` if it has none.
     *
     * @param key the key to look up.
     * @return the value of [key], or `null`.
     * @since 2.2.0
     */
    public operator fun get(key: K): V? {
        val ordinal = index.ordinalOf(key) ?: return null
        return ordinalValues[ordinal]
    }

    /**
     * Returns whether [key] has a value.
     *
     * @param key the key to look up.
     * @return `true` if [key] has a value, `false` otherwise.
     * @since 2.2.0
     */
    public operator fun contains(key: K): Boolean = get(key) != null

    /**
     * Gives [key] the value [value], replacing the value it had.
     *
     * @param key the key to set.
     * @param value the new value of [key].
     * @since 2.2.0
     */
    public operator fun set(key: K, value: V) {
        val ordinal = index.ordinalFor(key)
        if (ordinal == ordinalKeys.size) {
            ordinalKeys.add(key)
            ordinalValues.add(null)
        }
        if (ordinalValues[ordinal] == null) size++
        ordinalValues[ordinal] = value
    }

    /**
     * Returns the value of [key], giving it the value of [value] first if it has none.
     *
     * [value] is called at most once, and only if [key] has no value. If it throws, the map stays unchanged.
     *
     * @param key the key to look up or to set.
     * @param value gives the value of a key that has none.
     * @return the value [key] had or now has.
     * @since 2.2.0
     */
    public fun getOrPut(key: K, value: () -> V): V {
        val found = get(key)
        if (found != null) return found
        val created = value()
        set(key, created)
        return created
    }

    /**
     * Removes the value of [key], if it has one.
     *
     * @param key the key whose value to remove.
     * @return the value [key] had, or `null` if it had none.
     * @since 2.2.0
     */
    public fun remove(key: K): V? {
        val ordinal = index.ordinalOf(key) ?: return null
        val removed = ordinalValues[ordinal]
        ordinalValues[ordinal] = null
        if (removed != null) size--
        return removed
    }

    /**
     * Removes every key and value, so that the map is empty and the next key it is given comes first.
     *
     * @since 2.2.0
     */
    public fun clear() {
        index = OrderedIndex()
        ordinalKeys.clear()
        ordinalValues.clear()
        size = 0
    }
}
