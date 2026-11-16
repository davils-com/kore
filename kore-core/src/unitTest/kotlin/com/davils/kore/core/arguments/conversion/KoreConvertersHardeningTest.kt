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

package com.davils.kore.core.arguments.conversion

import com.davils.kore.core.arguments.conversion.fixture.Role
import com.davils.kore.core.arguments.conversion.fixture.Scale
import com.davils.kore.core.arguments.conversion.fixture.numbersInForeignDigits
import com.davils.kore.core.arguments.fixture.Stage
import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.text.fixture.hostileText
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.checkAll
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val CASES: Int = 300
private const val HALF_AN_HOUR: Int = 30
private const val SECRET: String = "hunter2"
private const val OUT_OF_RANGE: String = "too large or too small"
private const val NO_HOST: String = "does not name its host after //, which its scheme requires"
private const val NEGATIVE_ANSWER: Int = -42
private const val SEVEN: Long = 7L
private const val HALF: Double = 0.5
private const val NEGATIVE_THOUSANDS: Double = -2500.0
private const val HUNDREDTH: Double = 0.01

private fun <T> refused(converter: KoreConverter<T>, raw: String): KoreConversionException =
    shouldThrow<KoreConversionException> { converter.convert(raw) }

private val everyConverter: List<KoreConverter<*>> = listOf(
    KoreConverters.int,
    KoreConverters.long,
    KoreConverters.double,
    KoreConverters.boolean,
    KoreConverters.duration(),
    KoreConverters.uri(),
    KoreConverters.semanticVersion,
    KoreConverters.semanticVersionRange,
    KoreConverters.choice("low", "high"),
    KoreConverters.enum<Stage>(),
    KoreConverters.path(),
)

class KoreConvertersHardeningTest : FunSpec({
    context("a whole number") {
        test("written in digits of another script is refused") {
            checkAll(CASES, numbersInForeignDigits) { raw ->
                refused(KoreConverters.int, raw)
                refused(KoreConverters.long, raw)
                refused(KoreConverters.double, raw)
            }
        }

        test("with a leading plus sign is refused") {
            refused(KoreConverters.int, "+5")
            refused(KoreConverters.long, "+5")
            refused(KoreConverters.double, "+5")
        }

        test("longer than the numeric limit is refused before it is parsed") {
            val zeros = "0".repeat(KoreLimits.MAX_NUMERIC_TEXT_LENGTH + 1)

            refused(KoreConverters.int, zeros).message shouldBe "is not a valid integer"
            refused(KoreConverters.long, zeros).message shouldBe "is not a valid long"
            refused(KoreConverters.double, zeros).message shouldBe "is not a valid decimal number"
        }

        test("that does not fit its type is refused for its size") {
            refused(KoreConverters.int, "2147483648").message shouldContain OUT_OF_RANGE
            refused(KoreConverters.long, "-9223372036854775809").message shouldContain OUT_OF_RANGE
        }

        test("written in plain ASCII digits is read, negative or not") {
            KoreConverters.int.convert("-42") shouldBe NEGATIVE_ANSWER
            KoreConverters.long.convert("007") shouldBe SEVEN
        }
    }

    context("a decimal number") {
        test("that is not a number or not finite is refused") {
            listOf("NaN", "-NaN", "Infinity", "-Infinity").forEach { raw -> refused(KoreConverters.double, raw) }
        }

        test("that only overflows to infinity while it is read is refused") {
            refused(KoreConverters.double, "1e999").message shouldContain OUT_OF_RANGE
            refused(KoreConverters.double, "-1e999").message shouldContain OUT_OF_RANGE
        }

        test("written as a hexadecimal float or with a type suffix is refused") {
            listOf("0x1p3", "1d", "1f", "1_000", " 1", "1 ").forEach { raw -> refused(KoreConverters.double, raw) }
        }

        test("written in plain decimal notation is read") {
            KoreConverters.double.convert(".5") shouldBe HALF
            KoreConverters.double.convert("-2.5e+3") shouldBe NEGATIVE_THOUSANDS
            KoreConverters.double.convert("1E-2") shouldBe HUNDREDTH
        }
    }

    context("a duration") {
        test("that is infinite is refused") {
            refused(KoreConverters.duration(), "Infinity").message shouldBe "is not a finite duration"
            refused(KoreConverters.duration(), "PT9999999999999999H")
        }

        test("that is negative is refused") {
            refused(KoreConverters.duration(), "-5s").message shouldBe "is a negative duration"
            refused(KoreConverters.duration(), "PT-1S").message shouldBe "is a negative duration"
        }

        test("outside the declared bounds is refused") {
            val bounded = KoreConverters.duration(min = 1.seconds, max = 1.hours)

            refused(bounded, "500ms").message shouldBe "is shorter than 1s"
            refused(bounded, "2h").message shouldBe "is longer than 1h"
            bounded.convert("30m") shouldBe HALF_AN_HOUR.minutes
        }

        test("text beyond the numeric limit or outside printable ASCII is refused") {
            refused(KoreConverters.duration(), "1".repeat(KoreLimits.MAX_NUMERIC_TEXT_LENGTH) + "s")
            refused(KoreConverters.duration(), "١s")
        }

        test("bounds that are negative, infinite or reversed are refused when declared") {
            shouldThrow<IllegalArgumentException> { KoreConverters.duration(min = (-1).seconds) }
            shouldThrow<IllegalArgumentException> { KoreConverters.duration(min = Duration.INFINITE) }
            shouldThrow<IllegalArgumentException> { KoreConverters.duration(min = 1.hours, max = 1.seconds) }
        }
    }

    context("an enum") {
        test("matches the case of ASCII letters loosely") {
            KoreConverters.enum<Scale>().convert("KILO") shouldBe Scale.Kilo
            KoreConverters.enum<Role>().convert("aDmIn") shouldBe Role.Admin
        }

        test("is not matched through the case folding of other letters") {
            refused(KoreConverters.enum<Scale>(), "Kilo")
            refused(KoreConverters.enum<Role>(), "admın")
            refused(KoreConverters.enum<Role>(), "ADMİN")
            refused(KoreConverters.enum<Stage>(), "ſtaging")
        }
    }

    context("a choice") {
        test("is matched regardless of the case of ASCII letters and read as it was declared") {
            KoreConverters.choice("low", "high").convert("HiGh") shouldBe "high"
        }

        test("is not matched through the case folding of other letters") {
            refused(KoreConverters.choice("kilo"), "Kilo")
            refused(KoreConverters.choice("id"), "İD")
        }

        test("without anything to choose from is refused when declared") {
            shouldThrow<IllegalArgumentException> { KoreConverters.choice() }
        }

        test("whose values differ only in case is refused when declared") {
            shouldThrow<IllegalArgumentException> { KoreConverters.choice("on", "ON") }.message shouldContain "\"on\""
        }
    }

    context("a URI by default") {
        test("is read when it is an absolute https URI") {
            KoreConverters.uri().convert("HTTPS://api.example.com/v1") shouldBe URI("HTTPS://api.example.com/v1")
        }

        test("is refused with any other scheme") {
            val elsewhere = listOf("http://example.com", "file:/etc/passwd", "jar:file:/x.jar!/y", "ftp://example.com")

            elsewhere.forEach { raw ->
                refused(KoreConverters.uri(), raw).message shouldBe "does not use the scheme https"
            }
        }

        test("is refused when it is relative") {
            refused(KoreConverters.uri(), "/v1/users").message shouldBe "is not an absolute URI"
            refused(KoreConverters.uri(), "//example.com/v1").message shouldBe "is not an absolute URI"
        }

        test("is refused when it carries user information, without repeating it") {
            listOf("https://user:$SECRET@example.com/", "https://user:$SECRET@host_name/").forEach { raw ->
                val problem = refused(KoreConverters.uri(), raw)

                problem.message shouldBe "carries user information, which this parameter does not accept"
                problem.cause.shouldBeNull()
            }
        }

        test("is refused when its scheme needs a host that it does not name after two slashes") {
            val hostless = listOf(
                "https:user:$SECRET@evil.example/x",
                "https:/user:$SECRET@evil.example/x",
                "https:///user:$SECRET@evil.example/x",
                "HTTPS:evil.example",
                "https://?query",
                "https:?query",
            )

            hostless.forEach { raw ->
                val problem = refused(KoreConverters.uri(allowUserInfo = true), raw)

                withClue(raw) {
                    problem.message shouldBe NO_HOST
                    problem.message shouldNotContain SECRET
                }
            }
        }

        test("that is malformed names the parser's reason without chaining its exception") {
            val problem = refused(KoreConverters.uri(), "https://user:$SECRET@exa mple.com/")

            problem.message shouldNotContain SECRET
            problem.cause.shouldBeNull()
        }
    }

    context("a URI that was widened") {
        test("accepts what its caller allowed explicitly") {
            val lenient = KoreConverters.uri(
                schemes = setOf("http", "https"),
                requireAbsolute = false,
                allowUserInfo = true,
            )

            lenient.convert("http://user:pass@example.com") shouldBe URI("http://user:pass@example.com")
            lenient.convert("/relative/path") shouldBe URI("/relative/path")
        }

        test("to any scheme accepts every scheme") {
            KoreConverters.uri(schemes = emptySet()).convert("file:/etc/hosts") shouldBe URI("file:/etc/hosts")
        }

        test("to any scheme still refuses a scheme that needs a host without one") {
            val lenient = KoreConverters.uri(schemes = emptySet(), requireAbsolute = false, allowUserInfo = true)

            listOf("http:user:pass@evil.example", "ws:/evil.example", "ftp:///evil.example", "WSS:x").forEach { raw ->
                withClue(raw) { refused(lenient, raw).message shouldBe NO_HOST }
            }
        }

        test("to any scheme accepts an opaque URI whose scheme needs no host, without taking it for user information") {
            val opaque = "mailto:team@example.com"

            KoreConverters.uri(schemes = emptySet()).convert(opaque) shouldBe URI(opaque)
        }

        test("to a scheme that cannot be one is refused when declared") {
            shouldThrow<IllegalArgumentException> { KoreConverters.uri(schemes = setOf("ht tp")) }
        }
    }

    context("every built-in converter") {
        test("refuses hostile text only ever with a conversion exception") {
            checkAll(CASES, hostileText) { raw ->
                everyConverter.forEach { converter ->
                    val failure = runCatching { converter.convert(raw) }.exceptionOrNull()

                    withClue(converter.typeName) {
                        (failure == null || failure is KoreConversionException) shouldBe true
                    }
                }
            }
        }
    }
})
