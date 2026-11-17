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

/**
 * What kind of caller a [Principal] is.
 *
 * The kind is part of the identity of a caller: the same id with another kind is another caller. It describes the
 * caller and grants nothing by itself; access is decided by roles.
 *
 * @since 1.0.0
 */
public enum class PrincipalKind {
    /**
     * A person, such as somebody signed in to an application.
     *
     * @since 1.0.0
     */
    User,

    /**
     * A program that calls with credentials of its own, such as another service or a service account.
     *
     * @since 1.0.0
     */
    Service,

    /**
     * The application itself, for work that no outside caller started, such as a scheduled job.
     *
     * @since 1.0.0
     */
    System,
}
