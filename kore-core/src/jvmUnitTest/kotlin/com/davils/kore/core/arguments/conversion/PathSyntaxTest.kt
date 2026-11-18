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

import com.davils.kore.core.arguments.fixture.ThrowingInspector
import com.davils.kore.core.text.fixture.ESCAPE
import com.davils.kore.core.text.fixture.hostileText
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

private const val CASES: Int = 300
private const val NETWORK: String = "is a network path"
private const val DEVICE: String = "is a device path"
private const val RESERVED: String = "names a device that Windows reserves"
private const val STREAM: String = "names an alternate data stream"
private const val TRAILING: String = "has a name that ends in a dot or a space"
private const val CONTROL: String = "contains a control character"
private const val INVALID: String = "is not a valid path"

private fun onWindows(raw: String): String? = pathProblem(raw, PathFlavour.Windows)

private fun onPosix(raw: String): String? = pathProblem(raw, PathFlavour.Posix)

class PathSyntaxTest : FunSpec({
    context("a path on any platform") {
        test("that is empty is refused") {
            onPosix("") shouldBe "is empty"
            onWindows("") shouldBe "is empty"
        }

        test("that contains NUL or another control character is refused") {
            listOf("/srv/a\u0000b", "/srv/$ESCAPE[2J", "a\nb", "a\u0085b", "a\u007Fb").forEach { raw ->
                onPosix(raw) shouldBe CONTROL
                onWindows(raw) shouldBe CONTROL
            }
        }

        test("made of hostile text with a control character in it is always refused") {
            checkAll(CASES, hostileText) { raw ->
                val hasControl = raw.any(Char::isISOControl)

                withClue(raw) {
                    (onPosix(raw) == CONTROL) shouldBe hasControl
                    (onWindows(raw) == CONTROL) shouldBe hasControl
                }
            }
        }

        test("that is ordinary is accepted") {
            onPosix("/srv/data/settings.conf").shouldBeNull()
            onWindows("""C:\srv\data\settings.conf""").shouldBeNull()
            onWindows("relative/dir/../file.txt").shouldBeNull()
        }
    }

    context("a path on Windows") {
        test("that names a share on another machine is refused") {
            listOf("""\\attacker\share\x""", "//attacker/share/x", "\\/attacker\\share", "\\\\").forEach { raw ->
                onWindows(raw) shouldBe NETWORK
            }
        }

        test("in the device namespace is refused") {
            val devices = listOf("""\\.\PhysicalDrive0""", """\\?\GLOBALROOT\Device\x""", "//./COM1", """\??\C:\x""")

            devices.forEach { raw -> withClue(raw) { onWindows(raw) shouldBe DEVICE } }
        }

        test("that names a reserved device, with or without an extension, is refused") {
            val reserved = listOf(
                "CON", "prn", "Aux.txt", "C:\\dir\\nul.tar.gz", "COM1", "com9.log", "LPT1", "lpt0",
                "COM\u00B9", "LPT\u00B3.txt", "CONIN$", "conout$", "dir/CON .txt",
            )

            reserved.forEach { raw -> withClue(raw) { onWindows(raw) shouldBe RESERVED } }
        }

        test("whose name merely starts like a reserved device is accepted") {
            listOf("CONSOLE", "COM10", "LPT", "nullable.txt", "auxiliary").forEach { raw ->
                withClue(raw) { onWindows(raw).shouldBeNull() }
            }
        }

        test("that names an alternate data stream is refused") {
            listOf("file.txt:hidden", "C:\\dir\\file::\$DATA", "dir:stream\\x", "1:\\x").forEach { raw ->
                withClue(raw) { onWindows(raw) shouldBe STREAM }
            }
        }

        test("may start with a drive") {
            onWindows("C:\\file").shouldBeNull()
            onWindows("c:file").shouldBeNull()
        }

        test("whose name ends in a dot or a space is refused") {
            listOf("name.", "dir\\name ", "C:\\dir.\\file", "...").forEach { raw ->
                withClue(raw) { onWindows(raw) shouldBe TRAILING }
            }
        }

        test("that steps through the current or the parent directory is accepted") {
            onWindows(""".\a\..\b""").shouldBeNull()
        }
    }

    context("a path on POSIX") {
        test("that would be hazardous on Windows is an ordinary name") {
            listOf("CON", "file.txt:stream", """\\server\share""", "name.", "name ").forEach { raw ->
                withClue(raw) { onPosix(raw).shouldBeNull() }
            }
        }
    }

    context("a path converter") {
        test("refuses a network path before it touches the file system") {
            val converter = pathConverter(setOf(PathRequirement.Exists), ThrowingInspector, PathFlavour.Windows)

            val failure = shouldThrow<ConversionException> { converter.convert("""\\attacker\share\x""") }

            failure.message shouldBe NETWORK
        }

        test("refuses a control character before it touches the file system") {
            val converter = fileConverter(setOf(PathRequirement.Exists), ThrowingInspector, PathFlavour.Posix)

            shouldThrow<ConversionException> { converter.convert("/etc/passwd\u0000.png") }.message shouldBe CONTROL
        }

        test("refuses a lone surrogate on every platform and keeps a paired one") {
            listOf("a\uD800b", "a\uDC00b", "a\uD800").forEach { raw ->
                onPosix(raw) shouldBe INVALID
                onWindows(raw) shouldBe INVALID
            }
            onPosix("a\uD83D\uDE00b").shouldBeNull()
            onWindows("a\uD83D\uDE00b").shouldBeNull()
        }

        test("refuses text the platform cannot turn into a path without repeating it") {
            val failure = shouldThrow<ConversionException> { Converters.path().convert("a\uD800b") }

            failure.message shouldBe INVALID
            failure.cause.shouldBeNull()
        }
    }
})
