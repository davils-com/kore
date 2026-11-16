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

package com.davils.kore.core.validation.violation

import com.davils.kore.core.validation.fixture.Shape
import com.davils.kore.core.validation.violation.KoreViolationPath.Companion.ROOT
import com.davils.kore.core.validation.violation.KoreViolationPath.Companion.named
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val SINKS: String = "sinks"
private const val KTOR: String = "io.ktor"
private const val QUEUE_SIZE: String = "queueSize"

class KoreViolationPathTest : FunSpec({
    context("root") {
        test("renders as nothing and has no segment") {
            ROOT.toString() shouldBe ""
            ROOT.isRoot shouldBe true
        }

        test("changes nothing when added on either side") {
            val path = named(SINKS)

            path + ROOT shouldBe path
            ROOT + path shouldBe path
        }
    }

    context("rendering") {
        test("shows a single name on its own") {
            named(SINKS).toString() shouldBe SINKS
        }

        test("keeps names that are no identifiers, such as options and environment variables") {
            named("--port").toString() shouldBe "--port"
            named("APP_DATA_DIR").toString() shouldBe "APP_DATA_DIR"
        }

        test("joins names with dots and attaches indices without one") {
            val sink = named(SINKS).index(2)
            val queueSize = sink.name("async").name(QUEUE_SIZE)

            queueSize.toString() shouldBe "sinks[2].async.queueSize"
        }

        test("shows a map key in quoted brackets") {
            val logger = named("loggers").key(KTOR)
            val level = logger.name("level")

            level.toString() shouldBe "loggers[\"io.ktor\"].level"
        }

        test("names a property after the property it references") {
            named(Shape::queueSize).toString() shouldBe QUEUE_SIZE
        }
    }

    context("appending") {
        test("concatenates the segments of two paths") {
            val outer = named(SINKS).index(0)
            val inner = named("async").name(QUEUE_SIZE)

            (outer + inner).toString() shouldBe "sinks[0].async.queueSize"
        }

        test("never changes the path it started from") {
            val base = named(SINKS)

            base.index(1)

            base.toString() shouldBe SINKS
        }
    }

    context("equality") {
        test("treats paths built the same way as equal with the same hash") {
            val left = named(SINKS).index(2)
            val right = named(SINKS).index(2)

            left shouldBe right
            left.hashCode() shouldBe right.hashCode()
        }

        test("tells paths with different indices apart") {
            named(SINKS).index(1) shouldNotBe named(SINKS).index(2)
        }

        test("tells one name containing a dot apart from two names") {
            val oneSegment = named("a.b")
            val twoSegments = named("a").name("b")

            oneSegment.toString() shouldBe "[\"a.b\"]"
            twoSegments.toString() shouldBe "a.b"
            oneSegment shouldNotBe twoSegments
        }

        test("tells a key apart from a name of the same text") {
            ROOT.key("port") shouldNotBe named("port")
        }

        test("is never equal to something that is not a path") {
            named(SINKS).equals(SINKS) shouldBe false
        }
    }

    context("segments") {
        test("reject a negative index") {
            shouldThrow<IllegalArgumentException> { ROOT.index(-1) }
        }

        test("keep the value they were created with") {
            val sink = named(SINKS).index(0)
            val path = sink.key(KTOR)

            path.segments shouldBe listOf(
                KoreViolationPath.Segment.Name(SINKS),
                KoreViolationPath.Segment.Index(0),
                KoreViolationPath.Segment.Key(KTOR),
            )
        }
    }
})
