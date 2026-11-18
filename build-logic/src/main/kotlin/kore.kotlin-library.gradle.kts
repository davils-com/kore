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

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("kore.base")
    id("kore.dependency-locking")
    id("kore.git-hooks")
    id("kore.trivy")
    id("kore.assurance")
    id("kore.platform")
    id("kore.web-tools")
    id("kore.testing")
    id("kore.detekt")
    id("kore.coverage")
    id("kore.api-validation")
    id("kore.docs")
    id("kore.publish")
    id("kore.archives")
    id("kore.tool-classpaths")
}

kreate {
    assurance {
        advisories {
            configurations = setOf(
                "jvmCompileClasspath",
                "jvmRuntimeClasspath",
                "jvmUnitTestCompileClasspath",
                "jvmUnitTestRuntimeClasspath",
                "wasmJsCompileClasspath",
                "wasmJsRuntimeClasspath",
                "wasmJsTestCompileClasspath",
                "wasmJsTestRuntimeClasspath",
            )
        }
    }
}
