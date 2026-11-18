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

package com.davils.kore.core.version.interval

import com.davils.kore.core.Limits
import com.davils.kore.core.version.SemanticVersion
import com.davils.kore.core.version.SemanticVersionRange
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.lang.reflect.Method
import java.lang.reflect.Modifier

private const val MALFORMED_MESSAGE: String =
    "A version interval must hold a version and have bounds without build metadata, and only a release can be its " +
        "excluded lower bound"
private const val SUCCESSOR_MESSAGE: String =
    "Only a pre-release of at most ${Limits.MAX_VERSION_LENGTH} characters has a pre-release successor"
private const val CORE_WITH_SEPARATOR_LENGTH: Int = 6
private const val MANGLED: Char = '$'

private val highest: SemanticVersion = SemanticVersion(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
private val release: SemanticVersion = SemanticVersion(1)
private val candidate: SemanticVersion = SemanticVersion(1, preRelease = listOf("rc", "1"))
private val longest: SemanticVersion =
    SemanticVersion(1, preRelease = listOf("a".repeat(Limits.MAX_VERSION_LENGTH - CORE_WITH_SEPARATOR_LENGTH)))

private val versionTypes: Set<Class<*>> = setOf(
    SemanticVersion::class.java,
    SemanticVersionRange::class.java,
    VersionInterval::class.java,
    Any::class.java,
)

private fun malformed(start: SemanticVersion?, isStartExclusive: Boolean, end: SemanticVersion?) {
    val failure = shouldThrow<IllegalArgumentException> {
        VersionInterval(start = start, isStartExclusive = isStartExclusive, end = end)
    }
    failure.message shouldBe MALFORMED_MESSAGE
}

private fun internalCreators(type: Class<*>): List<Method> = type.declaredMethods.filter { method ->
    val isReachable = !Modifier.isPrivate(method.modifiers)
    isReachable && MANGLED in method.name && method.returnType in versionTypes
}

private fun visibleToJava(members: List<Method>): List<String> =
    members.filterNot(Method::isSynthetic).map(Method::toString)

class VersionIntervalChecksTest : FunSpec({
    context("an interval that Kore creates") {
        test("is refused when its excluded lower bound has no version above it") {
            malformed(start = highest, isStartExclusive = true, end = null)
        }

        test("is refused when no version lies between its bounds") {
            malformed(start = SemanticVersion(2), isStartExclusive = false, end = release)
            malformed(start = release, isStartExclusive = false, end = release)
            malformed(start = null, isStartExclusive = false, end = MINIMUM)
        }

        test("is refused when its excluded lower bound is not a release") {
            malformed(start = candidate, isStartExclusive = true, end = null)
            malformed(start = null, isStartExclusive = true, end = release)
        }

        test("is refused when a bound carries build metadata") {
            malformed(start = release.withBuild("b"), isStartExclusive = false, end = null)
            malformed(start = null, isStartExclusive = false, end = release.withBuild("b"))
        }

        test("is created when it holds a version and its bounds are well formed") {
            val interval = VersionInterval(start = release, isStartExclusive = true, end = null)

            interval.contains(release.withPreRelease("rc")) shouldBe false
            interval.contains(SemanticVersion(major = 1, minor = 0, patch = 1)) shouldBe true
            interval.toString() shouldBe "(1.0.0,)"
        }
    }

    context("the version steps inside Kore") {
        test("refuse numbers that no version has") {
            shouldThrow<IllegalArgumentException> { releaseFloor(listOf(-1)) }
            shouldThrow<IllegalArgumentException> { preReleaseFloor(listOf(1, -1)) }
            shouldThrow<IllegalArgumentException> { floorAbove(listOf(-1, -1)) }
        }

        test("give a pre-release successor only to a pre-release") {
            val failure = shouldThrow<IllegalArgumentException> { preReleaseSuccessor(release) }

            failure.message shouldBe SUCCESSOR_MESSAGE
        }

        test("take the successor of the longest pre-release once, but not of that successor again") {
            val next = preReleaseSuccessor(longest)

            next.preRelease.last() shouldBe "0"
            (next > longest) shouldBe true
            shouldThrow<IllegalArgumentException> { preReleaseSuccessor(next) }
        }
    }

    context("Java source") {
        test("cannot call an internal member that creates or exposes a version, a range or an interval") {
            val owners = listOf(
                SemanticVersion.Companion::class.java,
                SemanticVersionRange.Companion::class.java,
                VersionInterval::class.java,
            )
            for (owner in owners) {
                withClue(owner.name) { visibleToJava(internalCreators(owner)) shouldBe emptyList() }
            }
        }

        test("cannot call the steps that go past the length of a version") {
            val steps = listOf(::successor, ::preReleaseSuccessor).map { step -> step.name }
            val declared = Class.forName("com.davils.kore.core.version.interval.VersionStepsKt").declaredMethods

            visibleToJava(declared.filter { method -> method.name in steps }) shouldBe emptyList()
        }
    }
})
