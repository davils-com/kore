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

package com.davils.kore.core.dsl

/**
 * Builds a value of type [T], typically after a block of a DSL has configured it.
 *
 * The interface itself promises neither thread safety nor a particular result of a second call of [build]; every
 * implementation states its own. [ValidatingBuilder] is the implementation that validates what it builds.
 *
 * @param T the type of the value built.
 * @since 1.0.0
 */
public fun interface Builder<out T> {
    /**
     * Builds the value.
     *
     * @return the value built.
     * @since 1.0.0
     */
    public fun build(): T
}
