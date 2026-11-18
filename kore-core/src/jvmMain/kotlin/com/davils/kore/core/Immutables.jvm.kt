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

import java.util.List.copyOf as listCopyOf
import java.util.Map.copyOf as mapCopyOf
import java.util.Set.copyOf as setCopyOf

internal actual fun <T : Any> Collection<T>.immutableList(): List<T> = listCopyOf(this)

internal actual fun <T : Any> Collection<T>.immutableSet(): Set<T> = setCopyOf(this)

internal actual fun <K : Any, V : Any> Map<K, V>.immutableMap(): Map<K, V> = mapCopyOf(this)
