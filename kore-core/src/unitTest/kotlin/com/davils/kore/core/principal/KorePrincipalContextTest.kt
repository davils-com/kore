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

package com.davils.kore.core.principal

import com.davils.kore.core.principal.context.PrincipalElement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

private const val ALICE: String = "alice"
private const val BOB: String = "bob"
private const val ADMIN: String = "admin"
private const val LATER_MILLIS: Long = 20

private suspend fun savedContextOf(caller: KorePrincipal): CoroutineContext =
    withPrincipal(caller) { currentCoroutineContext().minusKey(Job) }

private suspend fun elementOf(caller: KorePrincipal): CoroutineContext.Element =
    withPrincipal(caller) { currentCoroutineContext()[PrincipalElement].shouldNotBeNull() }

class KorePrincipalContextTest : FunSpec({
    context("once its block has returned") {
        test("is current nowhere, not even in a context saved inside the block") {
            val saved = savedContextOf(principal(ALICE, ADMIN))

            withContext(saved) { currentPrincipal() }.shouldBeNull()
            shouldThrow<KorePrincipalMissingException> { withContext(saved) { requirePrincipal() } }
        }

        test("does not take the place of the principal of another block through a saved context") {
            val saved = savedContextOf(principal(ALICE, ADMIN))

            val seen = withPrincipal(principal(BOB)) { withContext(saved) { currentPrincipal() } }

            seen.shouldBeNull()
        }

        test("establishes nobody through its element, whether with withContext, async or launch") {
            val stolen = elementOf(principal(ALICE, ADMIN))

            withPrincipal(principal(BOB)) {
                var launched: KorePrincipal? = principal(BOB)
                launch(stolen) { launched = currentPrincipal() }.join()

                withContext(stolen) { currentPrincipal() }.shouldBeNull()
                val awaited = async(stolen) { currentPrincipal() }.await()
                awaited.shouldBeNull()
                launched.shouldBeNull()
            }
        }

        test("blocks no other principal from being established where its saved context is used") {
            val saved = savedContextOf(principal(ALICE, ADMIN))

            val seen = withContext(saved) { withPrincipal(principal(BOB)) { currentPrincipal() } }

            seen shouldBe principal(BOB)
        }
    }

    context("while its block runs") {
        test("reaches a coroutine launched in the block that outlives the code of the block") {
            var seen: KorePrincipal? = null

            withPrincipal(principal(ALICE)) {
                launch {
                    delay(LATER_MILLIS)
                    seen = currentPrincipal()
                }
            }

            seen shouldBe principal(ALICE)
        }

        test("reaches code that runs without cancellation or on another thread") {
            val executor = Executors.newSingleThreadExecutor()
            val seen = executor.asCoroutineDispatcher().use { otherThread ->
                withPrincipal(principal(ALICE)) {
                    val uncancellable = withContext(NonCancellable) { currentPrincipal() }
                    val elsewhere = withContext(otherThread) { currentPrincipal() }
                    listOf(uncancellable, elsewhere)
                }
            }

            seen shouldBe listOf(principal(ALICE), principal(ALICE))
        }

        test("reaches a coroutine that the block starts with its own context") {
            val seen = withPrincipal(principal(ALICE)) {
                val own = currentCoroutineContext().minusKey(Job)
                async(own) { currentPrincipal() }.await()
            }

            seen shouldBe principal(ALICE)
        }
    }
})
