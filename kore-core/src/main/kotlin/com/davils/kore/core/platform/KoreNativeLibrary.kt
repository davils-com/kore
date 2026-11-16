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

package com.davils.kore.core.platform

import com.davils.kore.core.annotation.DavilsInternal
import java.io.IOException

/**
 * A native library that is loaded on first use and then kept, loaded or missing, for the lifetime of this object.
 *
 * The first read of [api] or [failure] runs the loader; concurrent first readers wait for it instead of running it
 * again. A [LinkageError] such as [UnsatisfiedLinkError], [NoClassDefFoundError] or [ExceptionInInitializerError],
 * a [RuntimeException] such as a [SecurityException] and an [IOException] of the loader mean that the library is not
 * available: [api] is then `null`, [failure] holds the cause, and the library is never loaded again, so a missing
 * library costs one attempt rather than one per access. Every other throwable, such as an [OutOfMemoryError] or a
 * checked exception that is no [IOException], is not kept but thrown to the reader, and the next read runs the loader
 * again. The object is safe to share between threads.
 *
 * Nothing is logged, because Kore has no logger and never writes to the standard streams; read [failure] to report
 * why the library is missing. The failure is the throwable of the loader as it is, so its message may name paths of
 * the file system; escape and bound it like any other foreign text before it is shown.
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary: the package is exported, so any code on the class path can call it.
 *
 * @param Api the type of the binding.
 * @param loader loads the library on first use.
 * @since 1.0.0
 */
@DavilsInternal
public class KoreNativeLibrary<out Api : Any>(private val loader: KoreNativeLoader<Api>) {
    private val outcome: Result<Api> by lazy { loaded() }

    /**
     * The binding of the loaded library, or `null` if the library could not be loaded; the first read loads it.
     *
     * Throws every throwable of the loader that does not mean a missing library, such as an [OutOfMemoryError], as it
     * is and without keeping it.
     *
     * @since 1.0.0
     */
    public val api: Api? get() = outcome.getOrNull()

    /**
     * Why the library could not be loaded, or `null` if it is loaded; the first read loads it.
     *
     * The throwable is the one the loader threw, unchanged and unescaped. Throws every throwable of the loader that
     * does not mean a missing library, such as an [OutOfMemoryError], as it is and without keeping it.
     *
     * @since 1.0.0
     */
    public val failure: Throwable? get() = outcome.exceptionOrNull()

    private fun loaded(): Result<Api> = try {
        Result.success(loader.load())
    } catch (expectedLinkFailure: LinkageError) {
        Result.failure(expectedLinkFailure)
    } catch (expectedLoadFailure: RuntimeException) {
        Result.failure(expectedLoadFailure)
    } catch (expectedExtractionFailure: IOException) {
        Result.failure(expectedExtractionFailure)
    }
}
