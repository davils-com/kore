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
import com.davils.kore.core.immutableList as platformImmutableList
import com.davils.kore.core.immutableMap as platformImmutableMap
import com.davils.kore.core.immutableSet as platformImmutableSet

/**
 * Copies this collection into a list that no caller can change, for the Davils libraries that build on Kore.
 *
 * A public type of a Davils library stores its collections through this function, never through `toList()` or `+`,
 * whose results a caller could cast to a mutable list and change. On the JVM the copy is `java.util.List.copyOf`, which
 * throws an [UnsupportedOperationException] from every mutating method; on Wasm it is a read-only view of a private
 * copy without mutating methods. The copy keeps the order of the elements and is safe to share between threads.
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary.
 *
 * @param T the type of the elements, which cannot be nullable.
 * @return an unchangeable copy of this collection, in the same order.
 * @throws NullPointerException on the JVM, if a caller that bypasses the type system, such as Java code, passes a
 * collection that contains `null`.
 * @since 2.1.0
 */
@DavilsInternal
public fun <T : Any> Collection<T>.immutableList(): List<T> = platformImmutableList()

/**
 * Copies this collection into a set that no caller can change, for the Davils libraries that build on Kore.
 *
 * It behaves like [immutableList] and drops duplicate elements. The JVM copy is `java.util.Set.copyOf`, whose order of
 * iteration is unspecified; on Wasm the copy keeps the order of the first occurrences.
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary.
 *
 * @param T the type of the elements, which cannot be nullable.
 * @return an unchangeable copy of this collection without duplicates.
 * @throws NullPointerException on the JVM, if a caller that bypasses the type system passes a collection that
 * contains `null`.
 * @since 2.1.0
 */
@DavilsInternal
public fun <T : Any> Collection<T>.immutableSet(): Set<T> = platformImmutableSet()

/**
 * Copies this map into a map that no caller can change, for the Davils libraries that build on Kore.
 *
 * It behaves like [immutableList]. The JVM copy is `java.util.Map.copyOf`, whose order of iteration is unspecified; on
 * Wasm the copy keeps the order of this map.
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary.
 *
 * @param K the type of the keys, which cannot be nullable.
 * @param V the type of the values, which cannot be nullable.
 * @return an unchangeable copy of this map.
 * @throws NullPointerException on the JVM, if a caller that bypasses the type system passes a map with a `null` key
 * or value.
 * @since 2.1.0
 */
@DavilsInternal
public fun <K : Any, V : Any> Map<K, V>.immutableMap(): Map<K, V> = platformImmutableMap()
