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

package com.davils.kore.core.concurrency

import com.davils.kore.core.annotation.DavilsExperimental

/**
 * The state of an [AdmissionGate].
 *
 * @since 2.1.0
 */
@DavilsExperimental
public enum class GateState {
    /**
     * The gate admits new work.
     *
     * @since 2.1.0
     */
    Open,

    /**
     * The gate admits no new work and waits for the work it admitted, such as a service that shuts down gracefully.
     *
     * @since 2.1.0
     */
    Draining,

    /**
     * The gate admits no new work and no longer waits for anything.
     *
     * @since 2.1.0
     */
    Closed,
}
