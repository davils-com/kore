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

package com.davils.kore.core.architecture.mapper

import com.davils.kore.core.architecture.data.DataTransferObject
import com.davils.kore.core.architecture.domain.DomainModel

/**
 * Turns domain models into transfer objects: the direction in which data leaves the application.
 *
 * The mapper decides which data of a domain model crosses the boundary of the application, so it is the place to leave
 * out what must not leave it, such as secrets. As a functional interface, a mapper can be a lambda. A mapper that
 * accepts a supertype serves where a subtype is passed, and one that produces a subtype serves where a supertype is
 * expected.
 *
 * @param D the domain model the mapper reads.
 * @param T the transfer object the mapper produces.
 * @since 1.0.0
 */
public fun interface DtoMapper<in D : DomainModel, out T : DataTransferObject> {
    /**
     * Turns [domain] into the transfer object that carries it.
     *
     * @param domain the domain model to write.
     * @return the transfer object.
     * @since 1.0.0
     */
    public fun toDto(domain: D): T
}

/**
 * Turns every domain model of [domains] into its transfer object, in the order of iteration.
 *
 * The result is a new list that belongs to the caller. An exception of [DtoMapper.toDto] stops the mapping and is
 * passed on unchanged.
 *
 * @param D the domain model the mapper reads.
 * @param T the transfer object the mapper produces.
 * @param domains the domain models to write, iterated once.
 * @return the transfer objects, one for each domain model.
 * @since 1.0.0
 */
public fun <D : DomainModel, T : DataTransferObject> DtoMapper<D, T>.toDto(domains: Iterable<D>): List<T> =
    domains.map { domain -> toDto(domain) }
