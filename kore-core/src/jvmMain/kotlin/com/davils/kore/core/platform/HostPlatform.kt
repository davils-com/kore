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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.core.platform

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.asciiLowercase
import com.davils.kore.core.text.isPrintableAscii

private val architectures: Map<String, Architecture> = mapOf(
    "x86_64" to Architecture.X64,
    "amd64" to Architecture.X64,
    "x64" to Architecture.X64,
    "aarch64" to Architecture.Arm64,
    "arm64" to Architecture.Arm64,
    "x86" to Architecture.X86,
    "i386" to Architecture.X86,
    "i486" to Architecture.X86,
    "i586" to Architecture.X86,
    "i686" to Architecture.X86,
    "arm" to Architecture.Arm32,
    "arm32" to Architecture.Arm32,
    "armv7l" to Architecture.Arm32,
    "riscv64" to Architecture.RiscV64,
    "ppc64le" to Architecture.Ppc64Le,
    "s390x" to Architecture.S390X,
)

private val systemPrefixes: List<Pair<String, OperatingSystem>> = listOf(
    "linux" to OperatingSystem.Linux,
    "mac os x" to OperatingSystem.MacOs,
    "macos" to OperatingSystem.MacOs,
    "darwin" to OperatingSystem.MacOs,
    "windows" to OperatingSystem.Windows,
    "freebsd" to OperatingSystem.FreeBsd,
)

/**
 * The operating system and processor architecture of a host, for the Davils libraries that choose a native library.
 *
 * [current] reads the system properties `os.name` and `os.arch` of the JVM, and [of] reads any pair of such names,
 * matching the common aliases such as `amd64` and `x86_64` or `Mac OS X`. A name that matches nothing gives `null`, so
 * a caller that loads native code refuses an unknown host instead of guessing. Names longer than
 * [Limits.MAX_NAME_LENGTH] and names with a character outside printable ASCII are refused before they are compared.
 * Whether a Linux host uses glibc or musl is not told apart. A host platform is immutable, and equal to another of the
 * same system and architecture.
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary.
 *
 * @property operatingSystem the operating system.
 * @property architecture the processor architecture.
 * @since 2.1.0
 */
@DavilsInternal
public class HostPlatform private constructor(
    public val operatingSystem: OperatingSystem,
    public val architecture: Architecture,
) {
    /**
     * The name of the platform in the form `<system>-<architecture>`, such as `linux-x64` or `macos-arm64`, as native
     * libraries are commonly sorted by.
     *
     * @since 2.1.0
     */
    public val classifier: String get() = "${operatingSystem.id}-${architecture.id}"

    /**
     * Tells whether [other] is a host platform with the same system and architecture.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal platform.
     * @since 2.1.0
     */
    override fun equals(other: Any?): Boolean =
        other is HostPlatform && operatingSystem == other.operatingSystem && architecture == other.architecture

    /**
     * Returns a hash code derived from the system and the architecture, consistent with [equals].
     *
     * @return the hash code.
     * @since 2.1.0
     */
    override fun hashCode(): Int = classifier.hashCode()

    /**
     * Returns the [classifier] of the platform.
     *
     * @return the classifier, such as `linux-x64`.
     * @since 2.1.0
     */
    override fun toString(): String = classifier

    /**
     * Finds host platforms.
     *
     * @since 2.1.0
     */
    public companion object {
        /**
         * Returns the platform the JVM runs on, read from the system properties `os.name` and `os.arch` at each call.
         *
         * @return the platform, or `null` if the JVM reports a system or an architecture Kore does not know.
         * @since 2.1.0
         */
        public fun current(): HostPlatform? =
            of(osName = System.getProperty("os.name").orEmpty(), osArch = System.getProperty("os.arch").orEmpty())

        /**
         * Returns the platform that the names [osName] and [osArch] describe, in the form the JVM reports them.
         *
         * @param osName the name of the operating system, such as `Linux` or `Windows 11`.
         * @param osArch the name of the architecture, such as `amd64`.
         * @return the platform, or `null` if either name matches nothing Kore knows.
         * @since 2.1.0
         */
        public fun of(osName: String, osArch: String): HostPlatform? {
            val system = systemOf(osName)
            val architecture = architectureOf(osArch)
            if (system == null || architecture == null) return null
            return HostPlatform(operatingSystem = system, architecture = architecture)
        }

        private fun systemOf(name: String): OperatingSystem? {
            if (!isComparable(name)) return null
            val lowered = asciiLowercase(name)
            return systemPrefixes.firstOrNull { (prefix, _) -> lowered.startsWith(prefix) }?.second
        }

        private fun architectureOf(name: String): Architecture? {
            if (!isComparable(name)) return null
            return architectures[asciiLowercase(name)]
        }

        private fun isComparable(name: String): Boolean =
            name.length <= Limits.MAX_NAME_LENGTH && name.all(::isPrintableAscii)
    }
}
