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

package com.davils.kore.core.platform

import com.davils.kore.core.annotation.DavilsInternal

/**
 * An operating system that the JVM can report, for the Davils libraries that load native code.
 *
 * Marked [DavilsInternal]: entries will be added as Davils supports more systems, which changes every `when` over them.
 *
 * @property id the name of the system in a platform classifier, such as `linux`.
 * @since 2.1.0
 */
@DavilsInternal
public enum class OperatingSystem(public val id: String) {
    /**
     * Linux, glibc and musl alike.
     *
     * @since 2.1.0
     */
    Linux("linux"),

    /**
     * macOS.
     *
     * @since 2.1.0
     */
    MacOs("macos"),

    /**
     * Windows.
     *
     * @since 2.1.0
     */
    Windows("windows"),

    /**
     * FreeBSD.
     *
     * @since 2.1.0
     */
    FreeBsd("freebsd"),
}
