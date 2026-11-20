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
 * Hands out an ordinal for every distinct key it is given: `0` for the first key, `1` for the second and so on, and
 * the same ordinal again whenever a key comes back.
 *
 * Keys are found by [compareTo][Comparable.compareTo] in a balanced search tree and never by their hash code, so a
 * structure keyed by input, such as names, attributes or prefixes from a document, cannot be slowed down by keys an
 * attacker chose to collide. The tree stays balanced for keys in any order, sorted keys included, so the comparisons
 * of [ordinalOf] and [ordinalFor] grow with the logarithm of [size]; a comparison of text grows with the length of
 * the common prefix of the two keys. The index holds every key it was ever given until it is dropped; a caller that
 * fills it from input bounds the number of keys first.
 *
 * The order of the keys must be total and consistent: two keys compare as `0` exactly when they are the same key, and
 * a key must not change its order while the index holds it. An inconsistent order may lose keys or hand out a
 * second ordinal for the same key.
 *
 * An index is mutable and not safe to use from several threads at once without synchronization of the caller.
 *
 * @param K the type of the keys.
 * @since 2.2.0
 */
@DavilsInternal
public class OrderedIndex<K : Comparable<K>> {
    private var root: Node<K>? = null

    /**
     * The number of distinct keys this index has handed out an ordinal for, which is also the next ordinal.
     *
     * @since 2.2.0
     */
    public var size: Int = 0
        private set

    /**
     * Returns the ordinal of [key], or `null` if this index was never given it.
     *
     * @param key the key to look up.
     * @return the ordinal of [key] between `0` and [size] exclusive, or `null` if it has none.
     * @since 2.2.0
     */
    public fun ordinalOf(key: K): Int? {
        var current = root
        while (current != null) {
            val order = key.compareTo(current.key)
            if (order == 0) return current.ordinal
            current = childToward(current, order)
        }
        return null
    }

    /**
     * Returns the ordinal of [key], handing out the next one, [size], if this index was never given it.
     *
     * @param key the key to look up or to add.
     * @return the ordinal of [key].
     * @since 2.2.0
     */
    public fun ordinalFor(key: K): Int {
        val found = ordinalOf(key)
        if (found != null) return found
        root = inserted(root, key)
        size++
        return size - 1
    }

    private fun inserted(node: Node<K>?, key: K): Node<K> {
        if (node == null) return Node(key, size)
        val order = key.compareTo(node.key)
        if (order < 0) node.left = inserted(node.left, key)
        if (order > 0) node.right = inserted(node.right, key)
        return balanced(node)
    }

    private fun childToward(node: Node<K>, order: Int): Node<K>? {
        if (order < 0) return node.left
        return node.right
    }

    private fun balanced(node: Node<K>): Node<K> {
        node.measure()
        if (node.balance > 1) return leftHeavyFixed(node)
        return rightHeavyFixed(node)
    }

    private fun leftHeavyFixed(node: Node<K>): Node<K> {
        val left = checkNotNull(node.left)
        if (left.balance < 0) node.left = rotatedLeft(left)
        return rotatedRight(node)
    }

    private fun rightHeavyFixed(node: Node<K>): Node<K> {
        if (node.balance >= -1) return node
        val right = checkNotNull(node.right)
        if (right.balance > 0) node.right = rotatedRight(right)
        return rotatedLeft(node)
    }

    private fun rotatedRight(node: Node<K>): Node<K> {
        val pivot = checkNotNull(node.left)
        node.left = pivot.right
        pivot.right = node
        node.measure()
        pivot.measure()
        return pivot
    }

    private fun rotatedLeft(node: Node<K>): Node<K> {
        val pivot = checkNotNull(node.right)
        node.right = pivot.left
        pivot.left = node
        node.measure()
        pivot.measure()
        return pivot
    }

    private class Node<K>(val key: K, val ordinal: Int) {
        var left: Node<K>? = null

        var right: Node<K>? = null

        var height: Int = 1
            private set

        val balance: Int get() = heightOf(left) - heightOf(right)

        fun measure() {
            height = maxOf(a = heightOf(left), b = heightOf(right)) + 1
        }

        private fun heightOf(node: Node<K>?): Int = node?.height ?: 0
    }
}
