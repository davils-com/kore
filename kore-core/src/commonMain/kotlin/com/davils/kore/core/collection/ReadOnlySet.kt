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

internal class ReadOnlySet<T>(private val elements: Set<T>) : AbstractSet<T>() {
    override val size: Int get() = elements.size

    override fun contains(element: T): Boolean = element in elements

    override fun iterator(): Iterator<T> {
        val inner = elements.iterator()
        return object : Iterator<T> {
            override fun hasNext(): Boolean = inner.hasNext()

            override fun next(): T {
                if (!inner.hasNext()) throw NoSuchElementException("The iterator has no further element")
                return inner.next()
            }
        }
    }
}
