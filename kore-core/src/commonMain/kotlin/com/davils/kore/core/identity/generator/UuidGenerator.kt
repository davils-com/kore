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

package com.davils.kore.core.identity.generator

import com.davils.kore.core.identity.Uuid

/**
 * A source of identifiers that code takes as a dependency instead of minting them itself, so a test can hand in
 * identifiers it knows in advance.
 *
 * [UuidV4Generator] and [UuidV7Generator] are the generators for production. As a functional interface, a
 * generator can also be a lambda, such as `UuidGenerator { fixed }`, and the generator of one version serves where
 * a generator of any [Uuid] is expected. The `kore-testing` module offers a deterministic generator of version 7
 * identifiers for tests.
 *
 * An implementation that is shared between threads must be safe for concurrent calls of [next]; both generators of
 * Kore are.
 *
 * @param T the type of identifier the generator hands out.
 * @since 1.0.0
 */
public fun interface UuidGenerator<out T : Uuid> {
    /**
     * Hands out the next identifier.
     *
     * @return the next identifier of this generator.
     * @since 1.0.0
     */
    public fun next(): T
}
