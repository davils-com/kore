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

package com.davils.kore.testing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength

private const val SURROGATE_FRAGMENT_LENGTH: Int = 1

class HostileFragmentsTest : FunSpec({
    context("all") {
        test("lists every fragment once") {
            HostileFragments.all.toSet().size shouldBe HostileFragments.all.size
        }

        test("holds the attacks on logs and terminals") {
            val attacks = listOf(
                HostileFragments.FORGED_LOG_LINE,
                HostileFragments.CLEAR_SCREEN,
                HostileFragments.CLIPBOARD_WRITE,
                HostileFragments.RIGHT_TO_LEFT_OVERRIDE,
                HostileFragments.LONE_HIGH_SURROGATE,
                HostileFragments.LONE_LOW_SURROGATE,
            )

            attacks.all { attack -> attack in HostileFragments.all } shouldBe true
        }
    }

    context("the lone surrogates") {
        test("are single surrogates on every target") {
            HostileFragments.LONE_HIGH_SURROGATE shouldHaveLength SURROGATE_FRAGMENT_LENGTH
            HostileFragments.LONE_HIGH_SURROGATE.single().isHighSurrogate() shouldBe true
            HostileFragments.LONE_LOW_SURROGATE.single().isLowSurrogate() shouldBe true
        }
    }
})
