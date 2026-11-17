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

package com.davils.kore.core.principal

private const val MISSING_MESSAGE: String = "No principal in the coroutine context. Establish one with " +
    "withPrincipal(...) at the edge that verified the caller, before calling code that requires it."

/**
 * Thrown by [requirePrincipal] when code that needs a principal runs where no principal was established with
 * [withPrincipal].
 *
 * It is an [IllegalStateException] because a missing principal is a mistake in how the application is wired, such as
 * an entry point that never established one or a coroutine started outside the block of [withPrincipal]; the code
 * that needed the principal does not run. Only Kore creates it. Its message is constant text that names
 * [withPrincipal] as the fix and contains no data of any caller.
 *
 * @since 1.0.0
 */
public class PrincipalMissingException internal constructor() : IllegalStateException(MISSING_MESSAGE)
