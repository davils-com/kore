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
 * Marks API that the Davils libraries share with each other and that no other code may use.
 *
 * It requires an opt-in at [RequiresOptIn.Level.ERROR]: Kotlin code that uses a marked declaration without
 * `@OptIn(DavilsInternal::class)` does not compile. A marked declaration may change or disappear in any release, patch
 * releases included, without notice and without a deprecation cycle; the semantic version of a release does not cover
 * it.
 *
 * The marker is no access boundary. A marked declaration stays public in the class files, its package may be exported
 * by the Java module, any Kotlin code can opt in, and Java code neither sees the marker nor gets the error. It is not
 * visible through reflection at runtime either. Never rely on it to keep a caller away from a declaration for security
 * reasons.
 *
 * @since 1.0.0
 */
@RequiresOptIn(
    message = "Internal to the libraries of Davils. It may change or disappear in any release without notice.",
    level = RequiresOptIn.Level.ERROR,
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
public annotation class DavilsInternal
