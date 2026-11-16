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

package com.davils.kore.core.arguments

import com.davils.kore.core.arguments.conversion.KoreConversionException
import com.davils.kore.core.arguments.conversion.KoreConverter
import com.davils.kore.core.arguments.conversion.KoreConverters
import com.davils.kore.core.arguments.conversion.stringConverter
import com.davils.kore.core.arguments.declaration.Arity
import com.davils.kore.core.arguments.declaration.ParameterKind
import com.davils.kore.core.arguments.declaration.ValueRules
import com.davils.kore.core.arguments.declaration.valueParameter
import com.davils.kore.core.arguments.fixture.Breaking
import com.davils.kore.core.arguments.fixture.CancellingSource
import com.davils.kore.core.arguments.fixture.Exhausting
import com.davils.kore.core.arguments.fixture.FromSources
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.Simple
import com.davils.kore.core.arguments.fixture.Syntax
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import com.davils.kore.core.arguments.report.KoreParameterViolationCodes
import com.davils.kore.core.arguments.report.ParameterPresentation
import com.davils.kore.core.arguments.resolution.ParameterSlot
import com.davils.kore.core.validation.violation.KoreSeverity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.coroutines.cancellation.CancellationException

private const val LATE: String = "--late"
private const val NAME: String = "--name"
private const val PORT: String = "--port"

private fun infoOf(names: List<String>, isSensitive: Boolean, default: String?): KoreParameterInfo = KoreParameterInfo(
    names = names,
    environmentVariable = null,
    typeName = "int",
    kind = ParameterKind.Defaulted,
    presentation = ParameterPresentation(
        isSensitive = isSensitive,
        defaultDisplay = default,
        description = null,
        deprecation = null,
    ),
)

private fun syntaxFailure(vararg argv: String): KoreParameterViolation = shouldThrow<KoreParameterException> {
    Syntax().load(argv.toList(), NO_ENVIRONMENT)
}.violations.single()

class KoreParametersInternalsTest : FunSpec({
    context("a failure that is not the input's fault") {
        test("an assertion that fails in a converter is reported as a conversion failure") {
            val failure = shouldThrow<KoreParameterException> { Breaking().load(listOf("--value=x"), NO_ENVIRONMENT) }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            violation.cause.shouldBeInstanceOf<AssertionError>()
        }

        test("a failure of the machine thrown by a converter escapes the load") {
            shouldThrow<StackOverflowError> { Exhausting().load(listOf("--value=x"), NO_ENVIRONMENT) }
        }

        test("a cancellation thrown by a source escapes the load") {
            shouldThrow<CancellationException> {
                FromSources().load(emptyList(), listOf(CancellingSource()), NO_ENVIRONMENT)
            }
        }
    }

    context("a declaration") {
        test("made after loading started is refused") {
            val schema = Simple()
            schema.load(emptyList(), NO_ENVIRONMENT)
            val late = valueParameter(
                owner = schema,
                names = arrayOf(LATE),
                type = stringConverter,
                configure = {},
                arity = Arity.Optional(),
            )

            val failure = shouldThrow<IllegalStateException> { late.provideDelegate(schema, Simple::port) }

            failure.message shouldBe "Parameter 'port' was declared after load() had already started"
        }

        test("in an anonymous schema names no schema when it collides") {
            val failure = shouldThrow<IllegalArgumentException> {
                object : KoreParameters() {
                    val first: String? by optional(LATE)
                    val second: String? by optional(LATE)
                }
            }

            failure.message shouldBe "Parameter name '--late' is declared more than once in "
        }

        test("with only a short name derives no variable") {
            val schema = object : KoreParameters(environmentPrefix = "APP") {
                val isVerbose: Boolean by flag("-v")
            }

            schema.usage() shouldBe "Supported parameters:\n  -v"
        }
    }

    context("a description of a parameter created inside Kore") {
        test("refuses to be created without names or with a name outside the grammar") {
            listOf(emptyList(), listOf("port")).forEach { names ->
                shouldThrow<IllegalArgumentException> { infoOf(names = names, isSensitive = false, default = null) }
            }
        }

        test("keeps its own copy of the names it was given") {
            val names = mutableListOf(PORT)
            val info = infoOf(names = names, isSensitive = false, default = null)

            names += "--other"

            info.names shouldBe listOf(PORT)
        }

        test("masks the default of a sensitive parameter whatever it was given") {
            infoOf(names = listOf(PORT), isSensitive = true, default = "8080").defaultDisplay shouldBe "****"
            infoOf(names = listOf(PORT), isSensitive = false, default = "8080").defaultDisplay shouldBe "8080"
        }
    }

    context("the rules of a value created inside Kore") {
        test("refuse an empty split delimiter") {
            shouldThrow<IllegalArgumentException> {
                ValueRules(converter = stringConverter, validators = emptyList(), splitDelimiter = "")
            }
        }
    }

    context("a slot") {
        test("refuses to be read before it was assigned") {
            shouldThrow<IllegalStateException> { ParameterSlot<String>().value() }
        }
    }

    context("a converter") {
        test("written as a lambda is named as a plain value") {
            KoreConverter { raw -> raw }.typeName shouldBe "value"
        }

        test("brought by the schema carries the name it was given") {
            KoreConverters.custom("identifier") { raw -> raw }.typeName shouldBe "identifier"
        }

        test("failure keeps its message") {
            KoreConversionException("is wrong").message shouldBe "is wrong"
        }
    }

    context("the violations of the syntax") {
        test("name the type a missing value should have had") {
            val violation = syntaxFailure(NAME)

            violation.message shouldBe "expects a value of type string but none followed"
            violation.path.toString() shouldBe NAME
            violation.severity shouldBe KoreSeverity.Error
            violation.code shouldBe KoreParameterViolationCodes.MISSING_VALUE
        }

        test("say what is malformed") {
            val violation = syntaxFailure("--=x")

            violation.message shouldBe "has no parameter name"
            violation.path.toString() shouldBe "--"
        }

        test("say how often a single value was given") {
            syntaxFailure("--port=1", "--port=2").message shouldBe "was given 2 times but accepts a single value"
        }

        test("place a stray value by its position") {
            val violation = syntaxFailure("stray")

            violation.path.toString() shouldBe "argument 1"
            violation.message shouldBe "is a value with no parameter to belong to"
        }

        test("describe a parameter by its primary name") {
            val violation = syntaxFailure(NAME)

            violation.shouldBeInstanceOf<KoreParameterViolation.MissingValue>()
            violation.parameter.toString() shouldBe NAME
        }
    }
})
