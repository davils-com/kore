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
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val LINUX: String = "Linux"
private const val AMD64: String = "amd64"
private const val AARCH64: String = "aarch64"
private const val MAC: String = "Mac OS X"
private const val X86_64: String = "x86_64"
private const val LINUX_X64: String = "linux-x64"

private val platformCases: List<List<String>> = listOf(
    listOf(LINUX, AMD64, LINUX_X64),
    listOf(LINUX, AARCH64, "linux-arm64"),
    listOf(MAC, AARCH64, "macos-arm64"),
    listOf(MAC, X86_64, "macos-x64"),
    listOf("Windows 11", AMD64, "windows-x64"),
    listOf("Windows Server 2022", "arm64", "windows-arm64"),
    listOf("FreeBSD", AMD64, "freebsd-x64"),
    listOf(LINUX, "i686", "linux-x86"),
    listOf(LINUX, "armv7l", "linux-arm32"),
    listOf(LINUX, "riscv64", "linux-riscv64"),
    listOf(LINUX, "ppc64le", "linux-ppc64le"),
    listOf(LINUX, "s390x", "linux-s390x"),
)

class HostPlatformTest : FunSpec({
    context("of") {
        withData(platformCases) { (system, architecture, classifier) ->
            HostPlatform.of(osName = system, osArch = architecture)?.classifier shouldBe classifier
        }

        test("knows nothing it has not been told about") {
            HostPlatform.of(osName = "Plan 9", osArch = AMD64).shouldBeNull()
            HostPlatform.of(osName = LINUX, osArch = "loongarch64").shouldBeNull()
            HostPlatform.of(osName = "", osArch = "").shouldBeNull()
            HostPlatform.of(osName = "linux\u0000", osArch = "amd64\n").shouldBeNull()
        }

        test("refuses names beyond the longest name before comparing them") {
            val long = LINUX + "x".repeat(Limits.MAX_NAME_LENGTH)

            HostPlatform.of(osName = long, osArch = AMD64).shouldBeNull()
            HostPlatform.of(osName = LINUX, osArch = AMD64 + " ".repeat(Limits.MAX_NAME_LENGTH)).shouldBeNull()
        }
    }

    context("a platform") {
        test("is equal to another of the same system and architecture") {
            val first = HostPlatform.of(osName = LINUX, osArch = AMD64)
            val second = HostPlatform.of(osName = "linux", osArch = X86_64)

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe HostPlatform.of(osName = LINUX, osArch = AARCH64)
            first?.equals(LINUX_X64) shouldBe false
            first?.classifier shouldBe LINUX_X64
        }

        test("of the running JVM is known on the hosts Davils builds on") {
            val current = HostPlatform.current().shouldNotBeNull()

            current.operatingSystem.id.isNotEmpty() shouldBe true
            current.architecture.id.isNotEmpty() shouldBe true
        }
    }
})
