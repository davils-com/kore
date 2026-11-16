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

import com.davils.kore.core.architecture.data.KoreDataTransferObject
import com.davils.kore.core.architecture.domain.KoreDomainModel

/**
 * Turns transfer objects into domain models and back: a [KoreDomainMapper] and a [KoreDtoMapper] for the same pair of
 * types.
 *
 * Use it where data both enters and leaves the application in the same shape. Reading what was written should give
 * back an equal transfer object; Kore does not check that.
 *
 * @param D the domain model of the pair.
 * @param T the transfer object of the pair.
 * @since 1.0.0
 */
public interface KoreMapper<D, T> : KoreDomainMapper<T, D>, KoreDtoMapper<D, T>
    where D : KoreDomainModel, T : KoreDataTransferObject
