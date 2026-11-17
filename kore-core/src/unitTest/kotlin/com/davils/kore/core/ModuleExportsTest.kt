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

package com.davils.kore.core

import com.davils.kore.core.identity.internal.MonotonicUuidV7Generator
import com.davils.kore.core.principal.internal.PrincipalElement
import com.davils.kore.core.text.internal.SafeDisplay
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.lang.module.ModuleDescriptor

private const val MODULE_NAME: String = "com.davils.kore.core"

private val hiddenClasses: List<Class<*>> = listOf(
    SafeDisplay::class.java,
    PrincipalElement::class.java,
    MonotonicUuidV7Generator::class.java,
)

private fun exportedPackages(): Set<String> {
    val loader: ClassLoader = Limits::class.java.classLoader
    val locations = loader.getResources("module-info.class").toList()
    val kore = locations.map { url -> url.openStream().use(ModuleDescriptor::read) }
    val descriptor = kore.single { candidate -> candidate.name() == MODULE_NAME }
    return descriptor.exports().mapTo(mutableSetOf()) { export -> export.source() }
}

class ModuleExportsTest : FunSpec({
    context("the Java module of kore-core") {
        test("exports the packages with public API") {
            exportedPackages() shouldContain Limits::class.java.packageName
        }

        test("exports no internal package") {
            exportedPackages().none { name -> name.endsWith(".internal") } shouldBe true
        }

        test("keeps the sanitizer, the context element of principals and the monotonic generator from Java callers") {
            val exported = exportedPackages()
            hiddenClasses.forEach { hidden -> exported shouldNotContain hidden.packageName }
        }
    }
})
