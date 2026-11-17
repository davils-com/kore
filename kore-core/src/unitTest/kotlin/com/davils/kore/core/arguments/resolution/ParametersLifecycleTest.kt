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

package com.davils.kore.core.arguments.resolution

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.Simple
import com.davils.kore.core.arguments.report.ParametersNotLoadedException
import com.davils.kore.core.arguments.resolution.fixture.Dashes
import com.davils.kore.core.arguments.resolution.fixture.Duplicated
import com.davils.kore.core.arguments.resolution.fixture.LongShortName
import com.davils.kore.core.arguments.resolution.fixture.Nameless
import com.davils.kore.core.arguments.resolution.fixture.Undashed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val READER_THREADS: Int = 8
private const val READS_PER_THREAD: Int = 2000
private const val SHUTDOWN_SECONDS: Long = 10
private const val LOADED_PORT: Int = 4242
private const val ARRAY_PORT: Int = 9090

class ParametersLifecycleTest : FunSpec({
    context("before loading") {
        test("reading says so instead of pretending the value is missing") {
            val failure = shouldThrow<ParametersNotLoadedException> { Simple().port }

            failure.message shouldBe "Parameter 'port' was accessed before load() was called on its schema"
        }

        test("describing says so as well") {
            shouldThrow<ParametersNotLoadedException> { Simple().describe() }
        }

        test("the arguments after the terminator, the unknown ones and the warnings say so too") {
            val parameters = Simple()

            shouldThrow<ParametersNotLoadedException> { parameters.rest }
            shouldThrow<ParametersNotLoadedException> { parameters.unknown }
            shouldThrow<ParametersNotLoadedException> { parameters.warnings }
        }
    }

    context("loading") {
        test("is reflected by the loaded flag") {
            val parameters = Simple()

            parameters.isLoaded shouldBe false
            parameters.load(emptyList(), NO_ENVIRONMENT)
            parameters.isLoaded shouldBe true
        }

        test("twice is refused rather than silently ignored") {
            val parameters = Simple()
            parameters.load(listOf("--port=1"), NO_ENVIRONMENT)

            val failure = shouldThrow<IllegalStateException> { parameters.load(listOf("--port=2"), NO_ENVIRONMENT) }

            failure.message shouldContain "already loaded"
            parameters.port shouldBe 1
        }

        test("accepts what main is handed through the array overload") {
            val parameters = Simple()

            parameters.load(arrayOf("--port=9090"))

            parameters.port shouldBe ARRAY_PORT
        }
    }

    context("a loaded schema") {
        test("reads correctly from many threads at once") {
            val parameters = Simple()
            parameters.load(listOf("--port=4242"), NO_ENVIRONMENT)
            val pool = Executors.newFixedThreadPool(READER_THREADS)
            val mistakes = AtomicInteger()

            repeat(READER_THREADS) {
                pool.execute {
                    repeat(READS_PER_THREAD) { if (parameters.port != LOADED_PORT) mistakes.incrementAndGet() }
                }
            }
            pool.shutdown()

            pool.awaitTermination(SHUTDOWN_SECONDS, TimeUnit.SECONDS) shouldBe true
            mistakes.get() shouldBe 0
        }
    }

    context("declaring a parameter") {
        test("with a name without a dash is refused when the schema is built") {
            shouldThrow<IllegalArgumentException> { Undashed() }.message shouldContain "must start with a dash"
        }

        test("with a short name longer than one character is refused") {
            shouldThrow<IllegalArgumentException> { LongShortName() }.message shouldContain "exactly one character"
        }

        test("with nothing after its dashes is refused") {
            shouldThrow<IllegalArgumentException> { Dashes() }.message shouldContain "has no name after its dashes"
        }

        test("without any name is refused") {
            shouldThrow<IllegalArgumentException> { Nameless() }.message shouldBe "A parameter needs at least one name"
        }

        test("with a name another parameter declared is refused") {
            shouldThrow<IllegalArgumentException> { Duplicated() }.message shouldBe
                "Parameter name '--name' is declared more than once in Duplicated"
        }
    }
})
