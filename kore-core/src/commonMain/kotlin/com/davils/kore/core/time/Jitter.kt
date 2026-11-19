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

package com.davils.kore.core.time

/**
 * How a [Backoff] spreads its delays at random, so that many clients that failed at the same moment do not all retry
 * at the same moment again.
 *
 * @since 2.1.0
 */
public enum class Jitter {
    /**
     * No spread: every delay is exactly the computed one.
     *
     * @since 2.1.0
     */
    None,

    /**
     * Any delay from zero up to the computed one, which spreads retries the most.
     *
     * @since 2.1.0
     */
    Full,

    /**
     * Any delay from half the computed one up to the whole, which keeps at least half of the pause.
     *
     * @since 2.1.0
     */
    Equal,
}
