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

import com.davils.kore.core.text.escape.SafeDisplay
import java.nio.file.Path

/**
 * A condition that [KoreConverters.path] and [KoreConverters.file] check on a path when the schema is loaded.
 *
 * Every condition is checked once, at load time, against the file system as it is then, and is advisory: the returned
 * path is the path as given, not the real path that was checked, so the file system can change between the check and
 * the use of the path. [Exists], [Readable], [Directory] and [RegularFile] follow symbolic links and imply that the
 * path exists; [NoSymlinks] and [Within] do not, and accept a path that does not exist yet when its parent does. A
 * condition that cannot be decided, for example because the file system denies access, is not met.
 *
 * @since 1.0.0
 */
public sealed class KorePathRequirement {
    internal abstract val problem: String

    internal abstract val isExistenceImplied: Boolean

    internal abstract fun isMetBy(path: Path, inspector: PathInspector): Boolean

    /**
     * The path exists, following symbolic links, so a dangling link does not.
     *
     * @since 1.0.0
     */
    public data object Exists : KorePathRequirement() {
        override val problem: String get() = "does not exist"

        override val isExistenceImplied: Boolean get() = true

        override fun isMetBy(path: Path, inspector: PathInspector): Boolean = inspector.exists(path)
    }

    /**
     * The path exists and the process may read it, as far as the file system reports at load time.
     *
     * @since 1.0.0
     */
    public data object Readable : KorePathRequirement() {
        override val problem: String get() = "is not readable"

        override val isExistenceImplied: Boolean get() = true

        override fun isMetBy(path: Path, inspector: PathInspector): Boolean = inspector.isReadable(path)
    }

    /**
     * The path exists and is a directory, following symbolic links; it cannot be combined with [RegularFile].
     *
     * @since 1.0.0
     */
    public data object Directory : KorePathRequirement() {
        override val problem: String get() = "is not a directory"

        override val isExistenceImplied: Boolean get() = true

        override fun isMetBy(path: Path, inspector: PathInspector): Boolean = inspector.isDirectory(path)
    }

    /**
     * The path exists and is a regular file, following symbolic links; it cannot be combined with [Directory].
     *
     * @since 1.0.0
     */
    public data object RegularFile : KorePathRequirement() {
        override val problem: String get() = "is not a regular file"

        override val isExistenceImplied: Boolean get() = true

        override fun isMetBy(path: Path, inspector: PathInspector): Boolean = inspector.isRegularFile(path)
    }

    /**
     * No component of the path is a symbolic link.
     *
     * Every component of the absolute path, as written and before `..` is resolved, is checked without following
     * links, so a link anywhere on the way is refused, also one that a later `..` leaves again. A Windows junction or
     * another reparse point counts as a link, and a component whose kind cannot be read is refused. Other differences
     * between the path and its real path, such as a short Windows name or the case of a name on a case-insensitive
     * file system, are not links. A path that does not exist is accepted when it can still be created, which means
     * that its name is not `.` or `..` and its parent exists and passes the same check; a dangling link is refused.
     * The check is advisory and made once at load time, so a link created afterwards is not seen.
     *
     * @since 1.0.0
     */
    public data object NoSymlinks : KorePathRequirement() {
        override val problem: String get() = "passes through a symbolic link"

        override val isExistenceImplied: Boolean get() = false

        override fun isMetBy(path: Path, inspector: PathInspector): Boolean = hasNoSymlinks(path, inspector)
    }

    /**
     * The path lies inside the directory [base] or is that directory, after every symbolic link of both is resolved.
     *
     * The real path of the path must start with the real path of [base], compared by whole name components, so
     * `/srv/app-evil` is not within `/srv/app`. [base] must exist. A path that does not exist yet is accepted when its
     * name is not `.` or `..` and the real path of its parent lies inside [base]; a dangling link is refused. A
     * relative path or base is resolved against the working directory of the process. The check is advisory and made
     * once at load time: the converter returns the path as given, not the real path it checked, so a link or a rename
     * made after the load can still lead outside [base]. Two requirements are equal when their bases are equal paths,
     * compared as written, so `/srv/app` and `/srv/./app` are different requirements.
     *
     * @property base the directory the path is confined to.
     * @since 1.0.0
     */
    public class Within(public val base: Path) : KorePathRequirement() {
        override val problem: String get() = "is outside the directory it is confined to"

        override val isExistenceImplied: Boolean get() = false

        override fun isMetBy(path: Path, inspector: PathInspector): Boolean = isWithin(path, base, inspector)

        /**
         * Compares two confinements by their [base], as the paths are written.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Within] with an equal [base].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Within && base == other.base

        /**
         * Returns a hash code derived from the [base].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = base.hashCode()

        /**
         * Returns the requirement with its [base], escaped, such as `Within(/srv/app)`.
         *
         * @return the requirement as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Within(${SafeDisplay.text(base.toString())})"
    }
}
