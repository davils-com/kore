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
 * Marks a type as a data source: the part of the data layer that talks to one external system, such as a database,
 * a remote service or a file, and exchanges [KoreDataTransferObject]s with it.
 *
 * The marker declares no member and inherits nothing, so it brings no framework into a type and a class can carry it
 * beside its own supertype. Everything a data source receives from its system comes from outside the application and
 * may come from an attacker.
 *
 * @since 1.0.0
 */
public interface KoreDataSource
