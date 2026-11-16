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

package com.davils.kore.core.architecture.data

/**
 * Marks a type as a data transfer object: data in the shape in which it crosses the boundary of the application,
 * such as the body of a request or the row of a table.
 *
 * The marker declares no member and inherits nothing, so it brings no framework into a type and a class can carry it
 * beside its own supertype. A transfer object holds data as it arrived from outside, unchecked; a
 * [com.davils.kore.core.architecture.mapper.KoreDomainMapper] is the place to validate it before it becomes a
 * [com.davils.kore.core.architecture.domain.KoreDomainModel].
 *
 * @since 1.0.0
 */
public interface KoreDataTransferObject
