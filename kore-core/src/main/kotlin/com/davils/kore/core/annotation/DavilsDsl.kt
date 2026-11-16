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

package com.davils.kore.core.annotation

/**
 * Marks the builders and scopes of the Davils libraries as one DSL, so a block nested in a builder cannot reach an
 * enclosing builder by accident.
 *
 * It is a [DslMarker]: inside a lambda whose receiver carries this marker, the members of an enclosing receiver that
 * carries it as well are only reachable through an explicit `this@label`. A function of an outer builder that the
 * inner builder lacks is therefore a compile error inside the nested block instead of a call that silently changes
 * the outer builder. Kore puts it on its own builders and scopes, such as
 * [KoreValidationScope][com.davils.kore.core.validation.KoreValidationScope] and
 * [KoreValidatingBuilder][com.davils.kore.core.dsl.KoreValidatingBuilder]; a builder that is meant to nest with them
 * carries it on its class or on the receiver type of its builder lambda.
 *
 * The marker only guides the Kotlin compiler. It is written to the class files but is not visible through reflection
 * at runtime, and Java callers are not affected by it.
 *
 * @since 1.0.0
 */
@DslMarker
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class DavilsDsl
