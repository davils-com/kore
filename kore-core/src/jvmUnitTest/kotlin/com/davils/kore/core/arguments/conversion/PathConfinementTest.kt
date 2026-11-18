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

import com.davils.kore.core.arguments.conversion.fixture.ComponentInspector
import com.davils.kore.core.arguments.conversion.fixture.Confined
import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.report.ParameterException
import com.davils.kore.core.arguments.report.ParameterViolation
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

private fun withinConverter(base: Path): Converter<Path> = Converters.path(PathRequirement.Within(base))

private fun refusal(converter: Converter<Path>, path: Path): String? =
    shouldThrow<ConversionException> { converter.convert(path.toString()) }.message

class PathConfinementTest : FunSpec({
    fun realDirectory(): Path {
        val directory = tempdir().toPath()
        return directory.toRealPath()
    }

    context("a path confined to a base directory") {
        test("inside the base is accepted, whether it exists yet or not") {
            val root = realDirectory()
            val base = root.resolve("app").createDirectories()
            val existing = base.resolve("existing.txt").createFile()
            val converter = withinConverter(base)

            converter.convert(existing.toString()) shouldBe existing
            converter.convert(base.resolve(NOT_YET).toString()) shouldBe base.resolve(NOT_YET)
        }

        test("in a sibling whose name merely starts like the base is refused") {
            val root = realDirectory()
            val base = root.resolve("app").createDirectories()
            val evil = root.resolve("app-evil").createDirectories()

            refusal(withinConverter(base), evil.resolve(SECRET_FILE).createFile()) shouldBe OUTSIDE
            refusal(withinConverter(base), evil.resolve(NOT_YET)) shouldBe OUTSIDE
        }

        test("that climbs out of the base with dot-dot is refused") {
            val root = realDirectory()
            val base = root.resolve("app").createDirectories()
            root.resolve("outside.txt").createFile()

            refusal(withinConverter(base), base.resolve("../outside.txt")) shouldBe OUTSIDE
        }

        test("that leaves the base through a symbolic link is refused") {
            val root = realDirectory()
            val base = root.resolve("app").createDirectories()
            val outside = root.resolve("outside").createDirectories()
            outside.resolve(SECRET_FILE).createFile()
            val link = Files.createSymbolicLink(base.resolve("link"), outside)

            refusal(withinConverter(base), link.resolve(SECRET_FILE)) shouldBe OUTSIDE
            refusal(withinConverter(base), link.resolve(NOT_YET)) shouldBe OUTSIDE
        }

        test("that is a dangling link pointing outside the base is refused") {
            val root = realDirectory()
            val base = root.resolve("app").createDirectories()
            val dangling = Files.createSymbolicLink(base.resolve("dangling"), root.resolve("created-later.txt"))

            refusal(withinConverter(base), dangling) shouldBe OUTSIDE
        }

        test("is refused when the base itself does not exist") {
            val root = realDirectory()
            val missing = root.resolve("missing")

            refusal(withinConverter(missing), missing.resolve(FILE)) shouldBe OUTSIDE
        }
    }

    context("a path that must not pass through a symbolic link") {
        test("without any link is accepted, whether it exists yet or not") {
            val directory = realDirectory()
            val file = directory.resolve("plain.txt").createFile()
            val converter = Converters.path(PathRequirement.NoSymlinks)

            converter.convert(file.toString()) shouldBe file
            converter.convert(directory.resolve(NOT_YET).toString()) shouldNotBe null
        }

        test("that is a link or passes through one is refused") {
            val root = realDirectory()
            val deep = root.resolve("deep").createDirectories()
            val target = deep.resolve("target").createDirectories()
            val file = target.resolve(FILE).createFile()
            deep.resolve("elsewhere.txt").createFile()
            val linkedDirectory = Files.createSymbolicLink(root.resolve("linked"), target)
            val linkedFile = Files.createSymbolicLink(root.resolve("file-link"), file)
            val converter = Converters.path(PathRequirement.NoSymlinks)

            refusal(converter, linkedFile) shouldBe THROUGH_LINK
            refusal(converter, linkedDirectory.resolve(FILE)) shouldBe THROUGH_LINK
            refusal(converter, linkedDirectory.resolve(NOT_YET)) shouldBe THROUGH_LINK
            refusal(converter, linkedDirectory.resolve("../elsewhere.txt")) shouldBe THROUGH_LINK
        }
    }

    context("a path checked for links component by component") {
        test("counts no short name or other spelling of its real path as a link") {
            val inspector = ComponentInspector(present = setOf("data", "data/PROGRA~1", "data/PROGRA~1/app.txt"))

            hasNoSymlinks(inspector.pathOf("data/PROGRA~1/app.txt"), inspector) shouldBe true
            hasNoSymlinks(inspector.pathOf("data/PROGRA~1/new.txt"), inspector) shouldBe true
        }

        test("refuses a junction or any other reparse point on the way") {
            val present = setOf("data", "data/junction", "data/junction/app.txt")
            val inspector = ComponentInspector(present = present, links = setOf("data/junction"))

            hasNoSymlinks(inspector.pathOf("data/junction/app.txt"), inspector) shouldBe false
            hasNoSymlinks(inspector.pathOf("data/junction/new.txt"), inspector) shouldBe false
        }

        test("refuses a path whose parent does not exist") {
            val inspector = ComponentInspector(present = setOf("data"))

            hasNoSymlinks(inspector.pathOf("data/missing/new.txt"), inspector) shouldBe false
        }
    }

    context("a set of requirements") {
        test("that contradicts itself is refused when declared") {
            shouldThrow<IllegalArgumentException> {
                Converters.path(PathRequirement.Directory, PathRequirement.RegularFile)
            }
            val first = PathRequirement.Within(Path.of("/a"))
            val second = PathRequirement.Within(Path.of("/b"))
            shouldThrow<IllegalArgumentException> { Converters.file(first, second) }
        }

        test("that only confines a path does not demand that it exists") {
            val directory = realDirectory()

            withinConverter(directory).convert(directory.resolve("new.txt").toString()) shouldNotBe null
        }

        test("names a confinement by its base when printed") {
            PathRequirement.Within(Path.of(SERVED)).toString() shouldContain Path.of(SERVED).toString()
            PathRequirement.Within(Path.of(SERVED)) shouldBe PathRequirement.Within(Path.of(SERVED))
        }
    }

    context("a schema with a confined path") {
        test("reports a path outside its base as a conversion failure") {
            val root = realDirectory()
            val base = root.resolve("app").createDirectories()
            val evil = root.resolve("app-evil").createDirectories()

            val failure = shouldThrow<ParameterException> {
                Confined(base).load(listOf("--upload=${evil.resolve("x")}"), NO_ENVIRONMENT)
            }

            val violation = failure.violations.single()
            violation.shouldBeInstanceOf<ParameterViolation.ConversionFailure>()
            violation.reason shouldBe OUTSIDE
        }
    }
})
