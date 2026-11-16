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
 * Whether an application, or a part of it, works on its real data or on mock data.
 *
 * Kore attaches no behaviour to the modes; the application decides what each one means, typically which
 * implementation of a [KoreDataSource] is wired in. The names of the entries are stable, so a mode can serve as a
 * qualifier in configuration and dependency injection.
 *
 * @since 1.0.0
 */
public enum class KoreDataMode {
    /**
     * The real data: the data sources talk to the actual systems.
     *
     * @since 1.0.0
     */
    Live,

    /**
     * Mock data: the data sources serve prepared data instead, for example in tests, previews and demonstrations.
     *
     * @since 1.0.0
     */
    Mock,
}
