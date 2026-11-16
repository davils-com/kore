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

import com.davils.kore.core.arguments.conversion.fixture.Confined
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.KoreParameterException
import com.davils.kore.core.arguments.report.KoreParameterViolation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

private const val OUTSIDE: String = "is outside the directory it is confined to"
private const val THROUGH_LINK: String = "passes through a symbolic link"
private const val FILE: String = "file.txt"
private const val NOT_YET: String = "not-yet.txt"
private const val SECRET_FILE: String = "secret.txt"
private const val SERVED: String = "/srv/app"

private fun withinConverter(base: Path): KoreConverter<Path> = KoreConverters.path(KorePathRequirement.Within(base))

private fun refusal(converter: KoreConverter<Path>, path: Path): String? =
    shouldThrow<KoreConversionException> { converter.convert(path.toString()) }.message

class KorePathConfinementTest : FunSpec({
    context("a path confined to a base directory") {
        test("inside the base is accepted, whether it exists yet or not") {
            val root = tempdir().toPath()
            val base = root.resolve("app").createDirectories()
            val existing = base.resolve("existing.txt").createFile()
            val converter = withinConverter(base)

            converter.convert(existing.toString()) shouldBe existing
            converter.convert(base.resolve(NOT_YET).toString()) shouldBe base.resolve(NOT_YET)
        }

        test("in a sibling whose name merely starts like the base is refused") {
            val root = tempdir().toPath()
            val base = root.resolve("app").createDirectories()
            val evil = root.resolve("app-evil").createDirectories()

            refusal(withinConverter(base), evil.resolve(SECRET_FILE).createFile()) shouldBe OUTSIDE
            refusal(withinConverter(base), evil.resolve(NOT_YET)) shouldBe OUTSIDE
        }

        test("that climbs out of the base with dot-dot is refused") {
            val root = tempdir().toPath()
            val base = root.resolve("app").createDirectories()
            root.resolve("outside.txt").createFile()

            refusal(withinConverter(base), base.resolve("../outside.txt")) shouldBe OUTSIDE
        }

        test("that leaves the base through a symbolic link is refused") {
            val root = tempdir().toPath()
            val base = root.resolve("app").createDirectories()
            val outside = root.resolve("outside").createDirectories()
            outside.resolve(SECRET_FILE).createFile()
            val link = Files.createSymbolicLink(base.resolve("link"), outside)

            refusal(withinConverter(base), link.resolve(SECRET_FILE)) shouldBe OUTSIDE
            refusal(withinConverter(base), link.resolve(NOT_YET)) shouldBe OUTSIDE
        }

        test("that is a dangling link pointing outside the base is refused") {
            val root = tempdir().toPath()
            val base = root.resolve("app").createDirectories()
            val dangling = Files.createSymbolicLink(base.resolve("dangling"), root.resolve("created-later.txt"))

            refusal(withinConverter(base), dangling) shouldBe OUTSIDE
        }

        test("is refused when the base itself does not exist") {
            val root = tempdir().toPath()
            val missing = root.resolve("missing")

            refusal(withinConverter(missing), missing.resolve(FILE)) shouldBe OUTSIDE
        }
    }

    context("a path that must not pass through a symbolic link") {
        test("without any link is accepted, whether it exists yet or not") {
            val directory = tempdir().toPath()
            val file = directory.resolve("plain.txt").createFile()
            val converter = KoreConverters.path(KorePathRequirement.NoSymlinks)

            converter.convert(file.toString()) shouldBe file
            converter.convert(directory.resolve(NOT_YET).toString()) shouldNotBe null
        }

        test("that is a link or passes through one is refused") {
            val root = tempdir().toPath()
            val deep = root.resolve("deep").createDirectories()
            val target = deep.resolve("target").createDirectories()
            val file = target.resolve(FILE).createFile()
            deep.resolve("elsewhere.txt").createFile()
            val linkedDirectory = Files.createSymbolicLink(root.resolve("linked"), target)
            val linkedFile = Files.createSymbolicLink(root.resolve("file-link"), file)
            val converter = KoreConverters.path(KorePathRequirement.NoSymlinks)

            refusal(converter, linkedFile) shouldBe THROUGH_LINK
            refusal(converter, linkedDirectory.resolve(FILE)) shouldBe THROUGH_LINK
            refusal(converter, linkedDirectory.resolve(NOT_YET)) shouldBe THROUGH_LINK
            refusal(converter, linkedDirectory.resolve("../elsewhere.txt")) shouldBe THROUGH_LINK
        }
    }

    context("a set of requirements") {
        test("that contradicts itself is refused when declared") {
            shouldThrow<IllegalArgumentException> {
                KoreConverters.path(KorePathRequirement.Directory, KorePathRequirement.RegularFile)
            }
            val first = KorePathRequirement.Within(Path.of("/a"))
            val second = KorePathRequirement.Within(Path.of("/b"))
            shouldThrow<IllegalArgumentException> { KoreConverters.file(first, second) }
        }

        test("that only confines a path does not demand that it exists") {
            val directory = tempdir().toPath()

            withinConverter(directory).convert(directory.resolve("new.txt").toString()) shouldNotBe null
        }

        test("names a confinement by its base when printed") {
            KorePathRequirement.Within(Path.of(SERVED)).toString() shouldContain SERVED
            KorePathRequirement.Within(Path.of(SERVED)) shouldBe KorePathRequirement.Within(Path.of(SERVED))
        }
    }

    context("a schema with a confined path") {
        test("reports a path outside its base as a conversion failure") {
            val root = tempdir().toPath()
            val base = root.resolve("app").createDirectories()
            val evil = root.resolve("app-evil").createDirectories()

            val failure = shouldThrow<KoreParameterException> {
                Confined(base).load(listOf("--upload=${evil.resolve("x")}"), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<KoreParameterViolation.ConversionFailure>()
            violation.reason shouldBe OUTSIDE
        }
    }
})
