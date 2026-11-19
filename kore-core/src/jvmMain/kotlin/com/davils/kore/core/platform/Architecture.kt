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
 * A processor architecture that the JVM can report, for the Davils libraries that load native code.
 *
 * Marked [DavilsInternal]: entries will be added as Davils supports more architectures.
 *
 * @property id the name of the architecture in a platform classifier, such as `arm64`.
 * @since 2.1.0
 */
@DavilsInternal
public enum class Architecture(public val id: String) {
    /**
     * 64-bit x86, also called `amd64` and `x86_64`.
     *
     * @since 2.1.0
     */
    X64("x64"),

    /**
     * 64-bit ARM, also called `aarch64`.
     *
     * @since 2.1.0
     */
    Arm64("arm64"),

    /**
     * 32-bit x86.
     *
     * @since 2.1.0
     */
    X86("x86"),

    /**
     * 32-bit ARM.
     *
     * @since 2.1.0
     */
    Arm32("arm32"),

    /**
     * 64-bit RISC-V.
     *
     * @since 2.1.0
     */
    RiscV64("riscv64"),

    /**
     * 64-bit POWER in little-endian order.
     *
     * @since 2.1.0
     */
    Ppc64Le("ppc64le"),

    /**
     * 64-bit IBM Z.
     *
     * @since 2.1.0
     */
    S390X("s390x"),
}
