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

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

private const val THREADS: Int = 8

class NativeLibraryTest : FunSpec({
    context("api") {
        test("is the api of the loaded library") {
            val library = NativeLibrary { "api" }

            library.api shouldBe "api"
            library.failure.shouldBeNull()
        }

        test("is missing when the native library cannot be linked, and the failure says why") {
            val library = NativeLibrary<String> { throw UnsatisfiedLinkError("kore_native") }

            library.api.shouldBeNull()
            library.failure.shouldBeInstanceOf<UnsatisfiedLinkError>().message shouldBe "kore_native"
        }

        test("is missing when a class of the binding cannot be found") {
            val library = NativeLibrary<String> { throw NoClassDefFoundError("com/davils/Binding") }

            library.api.shouldBeNull()
            library.failure.shouldBeInstanceOf<NoClassDefFoundError>()
        }

        test("is missing when the loader fails at runtime, for example when it may not read the library") {
            val library = NativeLibrary<String> { throw SecurityException("no access") }

            library.api.shouldBeNull()
            library.failure.shouldBeInstanceOf<SecurityException>()
        }

        test("is missing when the library cannot be extracted") {
            val library = NativeLibrary<String> { throw IOException("disk full") }

            library.api.shouldBeNull()
            library.failure.shouldBeInstanceOf<IOException>()
        }
    }

    context("loading") {
        test("loads the library only once") {
            val loads = AtomicInteger(0)
            val library = NativeLibrary { loads.incrementAndGet() }

            library.api
            library.api
            library.failure

            loads.get() shouldBe 1
        }

        test("remembers a failure instead of loading again on every access") {
            val loads = AtomicInteger(0)
            val library = NativeLibrary<String> {
                loads.incrementAndGet()
                error("extraction failed")
            }

            library.api
            library.api
            library.failure

            loads.get() shouldBe 1
        }

        test("loads only once while many threads ask at the same time") {
            val loads = AtomicInteger(0)
            val library = NativeLibrary { loads.incrementAndGet() }
            val start = CountDownLatch(1)
            val seen = ConcurrentLinkedQueue<Int>()

            val workers = List(THREADS) {
                thread {
                    start.await()
                    seen += checkNotNull(library.api)
                }
            }
            start.countDown()
            workers.forEach(Thread::join)

            loads.get() shouldBe 1
            seen.toSet() shouldBe setOf(1)
        }

        test("never takes a virtual machine error for a missing library and passes it on") {
            val loads = AtomicInteger(0)
            val library = NativeLibrary<String> {
                loads.incrementAndGet()
                throw OutOfMemoryError("simulated")
            }

            shouldThrow<OutOfMemoryError> { library.api }
            shouldThrow<OutOfMemoryError> { library.failure }
            loads.get() shouldBe 2
        }
    }
})
