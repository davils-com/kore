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

import com.davils.kore.core.text.fixture.dataClassMembers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

class TextValueClassesTest : FunSpec({
    context("the value classes of text") {
        test("TextRange is a plain class without copy or component functions") {
            TextRange::class.java.dataClassMembers().shouldBeEmpty()
        }

        test("TextPosition is a plain class without copy or component functions") {
            TextPosition::class.java.dataClassMembers().shouldBeEmpty()
        }

        test("TextSnippet is a plain class without copy or component functions") {
            TextSnippet::class.java.dataClassMembers().shouldBeEmpty()
        }

        test("TextEdit is a plain class without copy or component functions") {
            TextEdit::class.java.dataClassMembers().shouldBeEmpty()
        }
    }
})
