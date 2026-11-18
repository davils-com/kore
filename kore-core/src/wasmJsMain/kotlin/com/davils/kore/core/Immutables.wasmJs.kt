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

package com.davils.kore.core

internal actual fun <T : Any> Collection<T>.immutableList(): List<T> {
    val elements = toList()
    return object : AbstractList<T>() {
        override val size: Int get() = elements.size

        override fun get(index: Int): T = elements[index]
    }
}

internal actual fun <T : Any> Collection<T>.immutableSet(): Set<T> {
    val elements = toSet()
    return object : AbstractSet<T>() {
        override val size: Int get() = elements.size

        override fun contains(element: T): Boolean = element in elements

        override fun iterator(): Iterator<T> = readOnly(elements.iterator())
    }
}

internal actual fun <K : Any, V : Any> Map<K, V>.immutableMap(): Map<K, V> {
    val copy = toMap()
    val entries = copy.map { (key, value) -> entryOf(key, value) }
    val entrySet = entries.immutableSet()
    return object : AbstractMap<K, V>() {
        override val entries: Set<Map.Entry<K, V>> get() = entrySet

        override fun get(key: K): V? = copy[key]

        override fun containsKey(key: K): Boolean = key in copy
    }
}

private fun <T> readOnly(iterator: Iterator<T>): Iterator<T> = object : Iterator<T> {
    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): T {
        if (!iterator.hasNext()) throw NoSuchElementException("The iterator has no further element")
        return iterator.next()
    }
}

private fun <K : Any, V : Any> entryOf(entryKey: K, entryValue: V): Map.Entry<K, V> = object : Map.Entry<K, V> {
    override val key: K = entryKey

    override val value: V = entryValue

    override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && other.key == key && other.value == value

    override fun hashCode(): Int = key.hashCode() xor value.hashCode()

    override fun toString(): String = "$key=$value"
}
