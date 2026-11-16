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

package com.davils.kore.core.architecture.domain

/**
 * One action of the application, called like a function: `renameServer(input)`.
 *
 * As a functional interface, a trivial use case can be a lambda. A use case that accepts a supertype serves where a
 * subtype is passed, and one that produces a subtype serves where a supertype is expected. Whether a use case is safe
 * to call from several coroutines at once, how it reacts to cancellation and what it throws are up to its
 * implementation.
 *
 * @param I the input of the action, [Unit] for an action without input.
 * @param O the result of the action.
 * @since 1.0.0
 */
public fun interface KoreUseCase<in I, out O> {
    /**
     * Performs the action for [input].
     *
     * @param input the input of the action.
     * @return the result of the action.
     * @since 1.0.0
     */
    public suspend operator fun invoke(input: I): O
}

/**
 * Performs a use case without input, so it is called with empty parentheses: `countServers()`.
 *
 * Same as calling the use case with [Unit].
 *
 * @param O the result of the action.
 * @return the result of the action.
 * @since 1.0.0
 */
public suspend operator fun <O> KoreUseCase<Unit, O>.invoke(): O = invoke(Unit)
