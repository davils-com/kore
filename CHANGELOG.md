# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- **`SafeText` for the libraries of Davils.** The `@DavilsInternal` object `SafeText` in
  `com.davils.kore.core.text` shows untrusted text escaped and shortened, exactly like the messages of Kore, so a
  library built on Kore escapes text at its sinks without a copy of the sanitizer. A caller can ask for at most
  `Limits.MAX_MESSAGE_LENGTH` code units, and `isSafe` refuses every value outside the range of code points.
- **`parseDecimalOrNull` for the libraries of Davils.** The `@DavilsInternal` function in `com.davils.kore.core`
  reads an ASCII decimal number to the nearest double with the same result on the JVM and on Wasm, where it reads
  through `Number` of JavaScript, and returns `null` for any other text, for text longer than the new
  `Limits.MAX_DECIMAL_TEXT_LENGTH` and for a value too large for a double.
- **Hostile text for the tests of code built on Kore.** `kore-testing` publishes `HostileFragments`, the pieces of
  text an attacker could send, and `unsafeCodePointsIn` and `isSafeToShow`, which judge what a renderer shows by
  their own list of invisible code points and surrogates and by the Unicode categories of the platform. The lone
  surrogates are values rather than constants, because the metadata of a Kotlin library would turn a constant into
  `U+FFFD` on Wasm.
- **Containment of foreign code.** `contained` and `attempted` in `com.davils.kore.core.failure` run a block and
  return its result or its throwable as a `Result`, like `runCatching`, but pass on what no caller may contain: a
  `CancellationException` and, on the JVM, a `VirtualMachineError` and an `InterruptedException`, the latter after
  setting the interrupt flag again. Every other `Error` is contained. `attempted` runs in a coroutine and passes on
  only the cancellation of its own caller; a cancellation from anywhere else becomes an `IllegalStateException` with
  it as the cause, so it cannot end the caller silently. `rethrowIfFatal` applies the same rule to a throwable.
- **`Outcome` for results with a typed failure.** The sealed interface `Outcome` in `com.davils.kore.core.outcome`
  is either a `Success` with a value or a `Failure` with a failure of any type, such as a sealed type of reasons, so
  an operation that can fail in known ways no longer needs a `Throwable` for them. It has exactly two kinds, which a
  `when` covers without `else`, and covariant type parameters. `map`, `mapFailure`, `flatMap`, `recover`, `fold`,
  `onSuccess`, `onFailure`, `getOrElse`, `getOrNull`, `failureOrNull` and `getOrThrow` work on it, and
  `Result.toOutcome` converts a `Result`. Its `toString` escapes the value and the failure.
- **Duplicates and unchangeable copies of collections.** `duplicates`, `duplicatesBy` and `firstDuplicateOrNull` in
  `com.davils.kore.core.collection` find the elements or keys that occur more than once, in the order of their second
  occurrence and in linear time. The `@DavilsInternal` functions `immutableList`, `immutableSet` and `immutableMap`
  give the Davils libraries the copies Kore stores its own public collections in, which no caller can change through
  a cast, and `kore-testing` uses them instead of a copy of its own.
- **Close matches for unknown text.** `editDistance`, `closestMatch` and `closeMatches` in
  `com.davils.kore.core.text` measure the Levenshtein distance between two texts by code points, stopping once a
  limit is passed, and suggest the candidates closest to a token for a hint such as "did you mean `--port`?". By
  default a token may be off by one edit for every three characters, at most three; `ignoreCase` folds ASCII letters
  only, so the result is the same on every platform and in every locale. The new `Limits.MAX_EDIT_DISTANCE_LENGTH`
  and `Limits.MAX_SUGGESTION_TOKEN_LENGTH` bound the work of a comparison. The suggestions of `Parameters` come from
  `closestMatch` now.
- **Rules for names without regular expressions.** `NameRule` in `com.davils.kore.core.naming` checks names and
  identifiers in one pass that grows linearly with the text, with the same result on the JVM and on Wasm and
  without the risk of a regular expression that backtracks or overflows the stack. `KEBAB`, `DOTTED`, `QUALIFIED`
  and `STRICT_KEBAB` accept exactly what `[a-z][a-z0-9-]*` and its dotted, qualified and strict variants accept;
  `nameRule { }` builds any other rule from its start, word and part separators, least number of parts, strictness
  and longest length, at most the new `Limits.MAX_NAME_LENGTH`. `requireValid` refuses a text with a message that
  escapes it and says why, for the `init` block of an identifier type.

### Changed

- **A value source no longer ends a load with a cancellation that is not the caller's.** A
  `CancellationException` that `ValueSource.read` throws while the loading coroutine is still active, such as one
  from an expired `withTimeout` inside the source, is reported as `ParameterViolation.SourceUnavailable` with an
  `IllegalStateException` as the cause. Before, it ended the load as if the caller had been cancelled, and no
  handler saw the failure. The cancellation of the caller itself still escapes unchanged.

## 2.0.0 - 2026-11-18

### Added

- **A `wasmJs` target.** `kore-core` and `kore-testing` are Kotlin Multiplatform libraries for the JVM and for
  WebAssembly in a JavaScript host. Everything that does not need the JDK is common code, and the same tests run on
  both targets, under Node.js on Wasm. On Wasm, the escaping of untrusted text reads the Unicode categories of the
  JavaScript engine, random bits come from `crypto.getRandomValues`, and `Parameters.load` reads `process.env` under
  Node.js and no environment in a browser.

### Changed

- **Breaking: the JVM-only members became extension functions of the JVM.** `Converters.uri`, `Converters.path` and
  `Converters.file` are extensions in `com.davils.kore.core.arguments.conversion`, `Uuid.toJavaUuid` and
  `Uuid.fromJavaUuid` extensions in `com.davils.kore.core.identity`. Kotlin code imports them; Java code calls them
  on `JvmConvertersKt` and `JavaUuidsKt`. They, `blocking` and `NativeLibrary` exist on the JVM only.
- **Breaking: Maven builds depend on `kore-core-jvm` and `kore-testing-jvm`.** `com.davils:kore-core` and
  `com.davils:kore-testing` are the multiplatform publications now, from which Gradle picks the JVM or the Wasm
  variant.
- **`RedactedException` no longer disables suppression.** It still has no cause, no suppressed exception when Kore
  creates it and, on the JVM, no stack trace.

## 1.0.3 - 2026-11-18

### Changed

- **Built with Kreate 5.2.0.** The Kotest Gradle plugin that Kreate now brings comes from the Gradle Plugin Portal,
  like `kotlinx-benchmark-plugin`, and KSP is verified by the signature of its Google release key.

## 1.0.2 - 2026-11-17

The first published version of Kore. It holds the code and the API of 1.0.0 and 1.0.1, which were tagged but
never published: the Windows check of 1.0.0 failed while Kreate 5.0.0 installed Trivy for several scans at once,
and the release guard of 1.0.1 could not see `origin/main` in the shallow checkout of the publish job.

### Changed

- **Released through the current pipeline of kotlin-workflow**, whose publish job checks out the whole history.

## 1.0.1 - 2026-11-17

Tagged but never published.

### Changed

- **Built with Kreate 5.0.2**, which installs Trivy once when parallel tasks find its cache empty and runs one
  Trivy scan at a time.

## 1.0.0 - 2026-11-17

The first version of Kore: the core building blocks of Davils as an open source library under the Apache License
2.0, built with Kreate 5.0.0 and published to `https://repo.davils.com`. The code comes from the core of an
internal Davils project and was hardened for use with untrusted input on the way.

### Added

- **`kore-core`** with semantic versions and npm-style ranges, UUIDs of version 4 and 7, validation with paths and
  stable violation codes, text positions, ranges, edits and snippets, command line and environment parameters with
  converters, groups and masking, principals with roles, tenants, attributes and delegation, data sizes, the
  architecture contracts and the annotations `DavilsInternal`, `DavilsExperimental`, `DavilsDsl`, `DavilsGenerated`
  and `DavilsPlatformBinding`.
- **`kore-testing`** with a deterministic UUID generator of version 7 and marked text for tests, kept out of
  `kore-core` so test helpers never reach a production class path.
- **`Limits`**, the documented upper bounds every entry point enforces before it does work that grows with its
  input.
- **A Java module** `com.davils.kore.core` that exports only packages with public API; `kore-testing` carries
  `Automatic-Module-Name: com.davils.kore.testing`.
- **`withPrincipal(principal, replacing = false) { }`**, the supported way to put a principal into the coroutine
  context; it refuses to replace a different principal unless asked to.
- **`ParameterLimits`**, `unknownNames`, the path requirements `Within(base)` and `NoSymlinks`, and the
  converters `uri(schemes, requireAbsolute, allowUserInfo)` and `duration(min, max)` with safe defaults.
- **`Principal.sameTenantAs`**, `SemanticVersion.isSamePrecedence` and `DataSize.times`.
- **`ValidatingBuilder.checkOpen()`**, which a builder calls first in every setter, so a change after its
  evaluation fails instead of being lost.

### Security

- **Untrusted text is escaped wherever it is shown.** Reports, violations, exception messages, usage texts,
  snippets and every `toString` escape control, format and separator characters, bidirectional overrides and lone
  surrogates and bound their length, so input can neither forge log lines nor control a terminal.
- **Sensitive values never leak.** A sensitive parameter has constant reasons, a redacted cause and no public
  detail; masks are always `****` and reveal neither length, count nor absence; userinfo of a URI is masked; a
  sensitive value on the command line raises a warning.
- **Input is bounded.** Arguments, values, versions, ranges, UUIDs, violations, nesting, principals, line maps and
  snippets are checked against `Limits` before any work; parsers are iterative and linear.
- **Converters are strict.** Numbers are ASCII only, doubles finite, durations finite and non-negative, enums and
  choices match ASCII case only, URIs default to absolute `https` without userinfo, and paths are checked lexically
  before any file system access.
- **Authorization fails closed.** `hasAllRoles` needs at least one role, delegation stays within a tenant and
  refuses cycles, attributes are sensitive by default and a principal shows only its id and kind.
- **Version ranges are exact.** Membership states its pre-release policy (`contains(version, policy)`, `SameCoreOnly`
  by default), the `in` operator is gone, and alternatives that name different pre-releases are no longer merged.
- **Concurrency is safe.** `blocking()` uses an explicit state machine, so a finished call is never cancelled and
  a failure never looks like a cancellation; UUIDs of version 7 are monotonic and use a `SecureRandom`.
- **Public collections are immutable**, even after a cast; public data classes are gone from the binary API.
- **Nothing secret is kept anywhere.** A violation about a sensitive parameter decides in its constructor what it
  keeps, so neither a field nor a getter, internal ones included, holds the secret or the words of a rule about it.
- **URIs name their host.** `uri()` refuses an `http`, `https`, `ws`, `wss` or `ftp` URI without an authority after
  `//`, such as `https:user:pass@host`, which browsers would read as one with credentials; shown values mask
  everything up to the last `@` after a scheme.
- **The lexer is bounded too.** No parameter keeps more occurrences than `maxValuesPerParameter` allows while the
  command line is read, so a bundle of short flags cannot exhaust the memory before the limit applies.
- **Foreign code stays contained.** Every `Error` of a converter, rule or source other than a `VirtualMachineError`
  is reported as a violation, and the name of a source is read through the same containment.
- **Invisible characters are escaped and refused.** Default-ignorable fillers and joiners such as U+3164 are shown
  as escapes, a leading combining mark cannot change the text before it, and principals refuse all of them.
- **Implementation details are not exported.** The sanitizer, the context element of principals and the monotonic
  UUID generator live in packages the Java module does not export.
