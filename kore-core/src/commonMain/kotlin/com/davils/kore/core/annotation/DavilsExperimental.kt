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
 * Marks API of the Davils libraries that may still change in a minor release.
 *
 * It requires an opt-in at [RequiresOptIn.Level.WARNING]: Kotlin code that uses a marked declaration without
 * `@OptIn(DavilsExperimental::class)` compiles with a warning, which fails every build that treats warnings as errors.
 * Opting in accepts that a later minor release may change the declaration incompatibly, so the semantic version of a
 * release does not protect code that uses it. A declaration that carries the marker itself instead of opting in passes
 * the requirement on to its own callers.
 *
 * The opt-in is checked by the Kotlin compiler only. Java code neither sees the marker nor gets the warning, and the
 * marker is not visible through reflection at runtime.
 *
 * @since 1.0.0
 */
@RequiresOptIn(
    message = "This API is experimental. It may still change in a minor release.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS,
)
@MustBeDocumented
public annotation class DavilsExperimental
