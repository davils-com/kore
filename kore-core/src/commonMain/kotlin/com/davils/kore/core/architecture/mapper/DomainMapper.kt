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
 * Turns transfer objects into domain models: the direction in which data enters the application.
 *
 * The mapper is where data that arrived from outside becomes a domain model, so it is the place to validate it and to
 * refuse what is invalid; Kore checks nothing on its way. As a functional interface, a mapper can be a lambda. A
 * mapper that produces a subtype serves where a supertype is expected, and one that accepts a supertype serves where
 * a subtype is passed.
 *
 * @param T the transfer object the mapper reads.
 * @param D the domain model the mapper produces.
 * @since 1.0.0
 */
public fun interface DomainMapper<in T : DataTransferObject, out D : DomainModel> {
    /**
     * Turns [dto] into the domain model it stands for.
     *
     * @param dto the transfer object to read, as it arrived from outside.
     * @return the domain model.
     * @since 1.0.0
     */
    public fun toDomain(dto: T): D
}

/**
 * Turns every transfer object of [dtos] into its domain model, in the order of iteration.
 *
 * The result is a new list that belongs to the caller. An exception of [DomainMapper.toDomain] stops the mapping
 * and is passed on unchanged. No limit of [com.davils.kore.core.Limits] applies, so the caller bounds the
 * number of transfer objects it accepts from outside.
 *
 * @param T the transfer object the mapper reads.
 * @param D the domain model the mapper produces.
 * @param dtos the transfer objects to read, iterated once.
 * @return the domain models, one for each transfer object.
 * @since 1.0.0
 */
public fun <T : DataTransferObject, D : DomainModel> DomainMapper<T, D>.toDomain(dtos: Iterable<T>): List<D> =
    dtos.map { dto -> toDomain(dto) }
