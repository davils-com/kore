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

package com.davils.kore.core.dsl

import com.davils.kore.core.dsl.fixture.LoggingBuilder
import com.davils.kore.core.dsl.fixture.ProbeBuilder
import com.davils.kore.core.dsl.fixture.QueueBuilder
import com.davils.kore.core.dsl.fixture.QueueData
import com.davils.kore.core.dsl.fixture.WarningsBuilder
import com.davils.kore.core.limit.Limits
import com.davils.kore.core.validation.ViolationCollector
import com.davils.kore.core.validation.fixture.paths
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.validateOrThrow
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val BROKEN: String = "the assembly is broken"
private const val DECLARATIONS: Int = 100_000

class ValidatingBuilderEvaluationTest : FunSpec({
    context("evaluating once") {
        test("assembles the value only once, however often it is validated or built") {
            val builder = ProbeBuilder()

            builder.validate()
            builder.validate()
            builder.build()
            builder.build()

            builder.assemblies shouldBe 1
        }

        test("hands back the same value and the same report every time") {
            val builder = ProbeBuilder()

            builder.build() shouldBeSameInstanceAs builder.build()
            builder.validate() shouldBeSameInstanceAs builder.validate()
        }

        test("reports a block nested while assembling only once, however often it is evaluated") {
            val builder = ProbeBuilder { queue { size = 0 } }

            builder.validate().paths() shouldContainExactly listOf("queue.size")
            val failure = shouldThrow<ValidationException> { builder.build() }

            failure.report.paths() shouldContainExactly listOf("queue.size")
        }

        test("builds a block nested while assembling after it was validated") {
            val builder = ProbeBuilder { queue { size = 1 } }

            builder.validate().isValid shouldBe true
            builder.build() shouldBe QueueData(QueueBuilder.DEFAULT_QUEUE_SIZE)
        }
    }

    context("after evaluating") {
        test("refuses every further declaration instead of ignoring it") {
            val builder = LoggingBuilder()
            builder.console()
            builder.validate()

            shouldThrow<IllegalStateException> { builder.file() }
                .message shouldContain "already evaluated"
            shouldThrow<IllegalStateException> { builder.logger("io.ktor") }
            builder.validate().isValid shouldBe true
        }

        test("refuses a nested block declared after it was built") {
            val builder = ProbeBuilder()
            builder.build()

            shouldThrow<IllegalStateException> { builder.queue() }
        }
    }

    context("evaluating in a broken state") {
        test("refuses to be evaluated again from inside its own assembly") {
            val builder = ProbeBuilder { validate() }

            shouldThrow<IllegalStateException> { builder.validate() }
                .message shouldContain "still being evaluated"
            builder.assemblies shouldBe 1
        }

        test("refuses to assemble again after its assembly failed") {
            val builder = ProbeBuilder { throw IllegalArgumentException(BROKEN) }

            shouldThrow<IllegalArgumentException> { builder.validate() }
                .message shouldBe BROKEN
            shouldThrow<IllegalStateException> { builder.build() }
            builder.assemblies shouldBe 1
        }
    }

    context("an assembly that refuses with a validation exception") {
        test("reports the problems of the refusal instead of throwing them") {
            val builder = ProbeBuilder { QueueData(0).validateOrThrow("queue") }

            val report = builder.validate()

            report.paths() shouldContainExactly listOf("size")
            report.isValid shouldBe false
            builder.validate() shouldBeSameInstanceAs report
        }

        test("has no value, so building throws with the problems of the refusal") {
            val builder = ProbeBuilder { QueueData(0).validateOrThrow("queue") }

            val failure = shouldThrow<ValidationException> { builder.build() }

            failure.report shouldBeSameInstanceAs builder.validate()
            builder.assemblies shouldBe 1
        }
    }

    context("an invalid value") {
        test("is kept to the builder, so not even its evaluation hands it out") {
            val builder = QueueBuilder()
            builder.size = 0

            val evaluation = builder.evaluate()

            evaluation.value.shouldBeNull()
            evaluation.report.paths() shouldContainExactly listOf("size")
        }
    }

    context("many warnings") {
        test("keep no value out as long as there are no more than the limit") {
            WarningsBuilder(Limits.MAX_VIOLATIONS)
                .build()
                .count shouldBe Limits.MAX_VIOLATIONS
        }

        test("keep the value out beyond the limit, because the problems left out count as an error") {
            val builder = WarningsBuilder(Limits.MAX_VIOLATIONS + 1)

            val failure = shouldThrow<ValidationException> { builder.build() }

            failure.report.errors
                .single()
                .code shouldBe ViolationCode.TOO_MANY
            failure.report.warnings shouldHaveSize Limits.MAX_VIOLATIONS
        }
    }

    context("violations adopted while blocks are declared") {
        test("are kept no more than a report holds, however many there are, and the rest are counted") {
            val builder = ProbeBuilder()
            repeat(DECLARATIONS) { builder.queue() }
            val type: Class<*> = ValidatingBuilder::class.java
            val field = type.getDeclaredField("adopted")
            field.isAccessible = true

            val kept = field.get(builder).shouldBeInstanceOf<ViolationCollector>()

            kept.violations() shouldHaveSize Limits.MAX_VIOLATIONS + 1
            val omission = builder.validate().violations.last()
            omission.message shouldBe
                "${DECLARATIONS - 1 - Limits.MAX_VIOLATIONS} further problems were found but not collected"
        }
    }
})
