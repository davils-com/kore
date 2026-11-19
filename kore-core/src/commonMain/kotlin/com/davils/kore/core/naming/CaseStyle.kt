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

package com.davils.kore.core.naming

import com.davils.kore.core.annotation.DavilsExperimental

/**
 * A way of writing a name of several words, such as `maxRetryCount` or `max-retry-count`.
 *
 * @since 2.1.0
 */
@DavilsExperimental
public enum class CaseStyle {
    /**
     * Words joined without a separator, every word but the first capitalised: `maxRetryCount`.
     *
     * @since 2.1.0
     */
    Camel,

    /**
     * Words joined without a separator, every word capitalised: `MaxRetryCount`.
     *
     * @since 2.1.0
     */
    Pascal,

    /**
     * Lower-case words joined by dashes: `max-retry-count`.
     *
     * @since 2.1.0
     */
    Kebab,

    /**
     * Lower-case words joined by underscores: `max_retry_count`.
     *
     * @since 2.1.0
     */
    Snake,

    /**
     * Upper-case words joined by underscores: `MAX_RETRY_COUNT`.
     *
     * @since 2.1.0
     */
    ScreamingSnake,

    /**
     * Lower-case words joined by spaces: `max retry count`.
     *
     * @since 2.1.0
     */
    Words,
}
