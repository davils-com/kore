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

package com.davils.kore.core.text

import com.davils.kore.core.Limits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.lang.reflect.InvocationTargetException

class LineMapConstructorTest : FunSpec({
    context("limits") {
        test("checks every instance, even one built through the constructor the compiler adds for its companion") {
            val constructors = LineMap::class.java.declaredConstructors.toList()
            val parameterTypes = constructors.flatMap { constructor -> constructor.parameterTypes.toList() }
            val synthetic = constructors.single { constructor -> constructor.isSynthetic }
            val huge = "x".repeat(Limits.MAX_LINE_MAP_LENGTH + 1)

            val refusal = shouldThrow<InvocationTargetException> { synthetic.newInstance(huge, null) }

            parameterTypes shouldNotContain IntArray::class.java
            refusal.cause.shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
})
