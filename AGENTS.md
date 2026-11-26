# AGENTS.md

Binding rules for every agent and every person working in this repository. Kore is the foundation library of
Davils: small, strict building blocks for Kotlin on the JVM and on WebAssembly (`wasmJs`) that other Davils libraries
and applications build on.
It is published under the Apache License 2.0 and is built to the standard it is used for, with security first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `kore-core` | `com.davils:kore-core` | Versions and ranges, UUIDs, URI references, validation and its common rules, text positions, snippets, diagnostics, line breaks and decoding, rules for names, close matches, the containment of foreign code, `Outcome`, collections, data sizes, admission and single flight, deadlines, guards and the arithmetic of backoffs, secure entropy, the masked process environment, command line and environment parameters, principals, the `Davils*` annotations and the architecture contracts |
| `kore-testing` | `com.davils:kore-testing` | Test support for code built on Kore: deterministic UUIDs, marked text, hostile text and clocks; never on a production class path |
| `kore-serialization` | `com.davils:kore-serialization` | Serializers of kotlinx-serialization for the values of Kore and `TextSerializer` for identifiers |
| `build-logic` | - | One convention plugin per Kreate concern, composed by `kore.kotlin-library` for the modules and `kore.root` for the root project; they hold nothing but `plugins { }` and `kreate { }` |

The root package of a module is `com.davils.kore.<module without kore->`, such as `com.davils.kore.core`, and its
Java module has the same name. `kore-core` declares it in `src/jvmMain/java/module-info.java` and exports only
packages with public API; every other module names it as `Automatic-Module-Name`.

## Targets

Every module is a Kotlin Multiplatform library with two targets: `jvm` and `wasmJs`. The Wasm tests run under Node.js;
the published klib runs in the browser as well.

- Code lives in `src/commonMain/kotlin` unless it needs the JVM. What only the JVM can offer, such as files, paths,
  URIs, threads, native libraries and the types of the JDK, lives in `src/jvmMain/kotlin`; a member of a common class
  that needs a JDK type becomes an extension function there.
- A platform difference hides behind an `internal expect fun` in `<Role>.kt` with its `actual` in `<Role>.jvm.kt` and
  `<Role>.wasmJs.kt`. Kore declares no `expect` or `actual` class: they are a Beta language feature.
- JavaScript interop is written with `js("...")` in `src/wasmJsMain/kotlin` only, behind a file-level
  `@file:OptIn(ExperimentalWasmJsInterop::class)`.

## Verify

```bash
./gradlew check
```

Nothing is done until it is green. `check` compiles with warnings as errors (Kotlin with progressive mode and
extra warnings, javac with `-Xlint:all -Werror`), runs Detekt with type resolution, the Kreate rule set and
`allRules`, both test modules, Kover verification at 97 % of lines, branches and instructions, the API and semver
check, the license header check, the changelog check, every Trivy scan and the Kreate assurance gate, which blocks
on every finding. The gate reads OSV.dev and deps.dev and Trivy downloads its database, so `check` needs the
network. The build runs on JDK 25; `gradle/gradle-daemon-jvm.properties` pins the daemon to it and toolchains are
never downloaded.

## Code rules

Enforced by Detekt with the Kreate rule set, the `libraries` rule set, `allRules` and warnings as errors, without
a baseline. Build scripts and `build-logic` follow the same rules, although Detekt does not analyse them.

- **License header first.** Every `.kt`, `.kts`, `.java`, `.properties` and YAML file starts with the Apache 2.0
  header, byte for byte as in the existing files; only the files Gradle generates in `gradle/` are exempt.
  `kreateLicenseHeaderCheck` and Detekt enforce it.
- **No comments.** No `//`, no `/* */` other than the license header. Names, types and tests carry the intent.
- **KDoc on public API only**, with `@param`, `@return`, `@throws` and `@since`, and never on internal or private
  declarations; protected members of public open or abstract classes are API and get KDoc. A new declaration gets
  the version it first ships in; existing `@since` values never change. KDoc states what a caller must know:
  behaviour on invalid input, the limits that apply, thread safety and every security consideration.
- **No `else`.** Return early; a `when` over a sealed type or an enum is exhaustive without `else ->`.
- **At most two chained calls.** Name intermediate results and computed conditions.
- **One top-level type per file**, named after it; a file of functions is named after its role.
- **No package for a single file.** A subpackage holds at least two files. A file that would stand alone in a
  package lives in the parent package instead, and when a subpackage shrinks to one file, that file moves up and the
  subpackage goes away. Exceptions are the root package of a module and an `internal` package, which may hold a
  single file.
- **No `!!`, no `lateinit`.** Model absence as a type or return early.
- **Explicit API.** Everything is `internal` unless a consumer needs it.
- **Names.** No type, file, function or property carries the name of the library as a prefix: `SemanticVersion`,
  never `KoreSemanticVersion`. The package says where a declaration comes from. Where a name meets a type of the same
  name, the foreign type is imported with an alias, such as `import kotlin.uuid.Uuid as KotlinUuid`, and a KDoc link
  names it in full. The only prefix is `Davils` on the annotations shared by every Davils library
  (`DavilsInternal`, `DavilsExperimental`, `DavilsDsl`, `DavilsGenerated`, `DavilsPlatformBinding`). The ids of the
  Gradle plugins in `build-logic` keep their `kore.` prefix.
- **`@DavilsInternal`** marks API shared between Davils libraries that consumers must not use; it fails the build of
  every caller that does not opt in. Once another Davils library uses such a declaration, it changes only in a major
  version, as [Public API](#public-api) describes. **`@DavilsExperimental`** marks API that may still change.
- **Immutable values.** Public types are immutable; public collections are built with `immutableList()`,
  `buildList` and the like, never stored from `toList()` or `+`, so a cast cannot change them.
- **No public `data class`.** It freezes `copy` and `componentN` into the binary API. Write a regular class with
  `equals`, `hashCode` and `toString`; `value class` stays allowed.
- **Explicit return types** on public functions with an expression body or a result; a block body that returns
  nothing omits `: Unit`, which the compiler rejects as redundant.
- **Kotlin first**, Java only where Kotlin offers no equivalent. Atomics come from `kotlin.concurrent.atomics`, through
  a local `@OptIn(ExperimentalAtomicApi::class)`.
  Experimental Kotlin API is allowed only through a local `@OptIn` and never appears in a public signature.
- **Shared building blocks first.** Foreign code, such as a callback, a converter or a value source, runs through
  `contained` or `attempted`, never through a bare `runCatching`, which swallows cancellations. The grammar of a name
  is checked by a `NameRule` or a hand-written scanner, never by a `Regex`, whose engine differs between the JVM and
  Wasm and may backtrack. A result with a typed failure is an `Outcome`, waiting for admitted work goes through an
  `AdmissionGate`, and a duplicate check uses `duplicates`. A copy of one of them is refactored onto it.
- **Nothing global.** No mutable singletons, no static configuration, no threads or timers of the library's own. The
  one sanctioned exception is the process-wide monotonic state of `UuidV7Generator`, documented in its KDoc.
  Detekt does not enforce this rule; reviews do.

## Security rules

Every input of Kore may come from an attacker: arguments, the environment, version strings, UUIDs, text, paths and
the attributes of principals. Kore is built so that a mistake of a caller fails closed.

- **Limits before work.** Every entry point checks its input against `Limits` before it does work that grows
  with the input, and refuses what is too large with a violation or an exception. Nothing is truncated silently.
  A new limit goes into `Limits`, never into a private constant.
- **Escape untrusted text exactly once, at the sink.** Whatever shows text that may come from input, such as an
  exception message, a `toString`, a usage text or a report, writes it through the internal `SafeDisplay`, which
  escapes control, format and separator characters, bidirectional overrides and lone surrogates and bounds the
  length. Canonical values with a restricted alphabet, such as versions and UUIDs, are shown as they are.
- **Secrets never leak.** A sensitive value never appears in a message, a reason, a cause, a `toString` or a
  report. The only mask is `SafeDisplay.REDACTED`; it reveals neither length nor count.
- **No raw input in exceptions.** Messages are constant text plus escaped, bounded fields; a thrown exception of a
  callback that may carry input is replaced by a redacted placeholder.
- **Fail closed.** An empty set of roles never passes, a missing principal is an error, an unknown argument is an
  error, a path check that cannot decide refuses.
- **Hide what Java must not reach.** Kotlin's `internal` is `public` in the bytecode, so a Java caller on the module
  path reaches every internal class of an exported package. A class whose misuse weakens a guarantee, such as the
  sanitizer, the context element of principals or the monotonic UUID state, lives in the `internal` subpackage of
  its package, for example `com.davils.kore.core.text.internal`, and `module-info.java` never exports an `internal`
  package.
- **Secure randomness only.** No `java.util.Random`, `kotlin.random.Random` or `ThreadLocalRandom`. Random bits come
  from `SecureEntropy` of `com.davils.kore.core.random`: `SecureRandom` on the JVM, `crypto.getRandomValues` on Wasm.
  Every Davils library draws its random bits from it, or from an `Entropy` a test passes, never from a copy of its own.
- **Every sink has an injection test.** A renderer is tested with the `hostileText` fixture and
  `shouldBeSafeToShow`; a sensitive value has a property test that it appears nowhere.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library; fakes come from `kore-testing` or a `fixture`.
- One `context` per behaviour or function; test names read as sentences.
- Tests live in `src/commonUnitTest/kotlin`, which runs on the JVM and on Wasm, in the package of the code they test,
  even when that package holds a single test. A test that needs the JVM, through reflection, threads, files, Java
  serialization or a JDK type, lives in `src/jvmUnitTest/kotlin`; a test of common code stays common wherever it can. A fixture lives in the `fixture` folder of the only subpackage that uses it, or in the shared `fixture` folder
  of the area; a `fixture` folder holds at least two files, so a lone fixture lives next to the tests that use it.
  `src/test` is rejected by the build.
- Property tests with adversarial generators for every parser and every security invariant.
- Every change ships with its tests; a fixed weakness ships with a test that would have caught it.

## Build

- Every module applies exactly one plugin, `id("kore.kotlin-library")`, and declares nothing but its description
  and its dependencies, through the configurations of the source sets such as `commonMainApi`. Detekt, Kover, Kotest, Trivy, the compiler and publishing are configured only in
  `build-logic`, through Kreate.
- `build-logic` holds no Kotlin classes, no tasks of its own and no Gradle API calls beyond reading the version
  catalog. What Kreate cannot express yet is added to Kreate, never programmed here.
- Every plugin in `build-logic` configures one Kreate concern and applies `kore.base`:

  | Plugin | Configures |
  |---|---|
  | `kore.base` | The Kreate plugin, the group and the version |
  | `kore.dependency-locking` | Strict locking of every configuration |
  | `kore.git-hooks` | The Git hooks |
  | `kore.trivy` | Every Trivy scan |
  | `kore.assurance` | The assurance gate and its `ignore` rules |
  | `kore.platform` | The JDK, the Kotlin compiler, the targets and the Java module name |
  | `kore.web-tools` | Node.js and Yarn for the Wasm target, from the repositories of the settings |
  | `kore.testing` | Kotest and the test suites |
  | `kore.detekt` | Detekt and its reports |
  | `kore.coverage` | Kover and its verification |
  | `kore.api-validation` | The API dump and the semver check |
  | `kore.docs` | Dokka |
  | `kore.publish` | The POM, the release guard and the Davils repository |
  | `kore.archives` | The legal files and the manifest of every archive |
  | `kore.tool-classpaths` | The version floors on the class paths of the build tools |
  | `kore.changelog` | The changelog check |
  | `kore.license-header` | The license header check |

  `kore.kotlin-library` composes them for a module and adds only the class paths the assurance gate reads;
  `kore.root` composes them for the root project. A new concern gets a plugin of its own, never a block in a
  composing plugin.
- `kore-serialization` depends on `kotlinx-serialization-core` and its tests on `kotlinx-serialization-json` and
  `kore-testing`, declared through the test configurations that Kreate creates late; its serializers are written by
  hand, so no module applies the serialization compiler plugin.
- Dependencies are declared in `gradle/libs.versions.toml`. Libraries of JetBrains may be added when they are
  needed; every other dependency needs the approval of a maintainer.
- Only `repo.davils.com` serves `com.davils` and its subgroups, through `exclusiveContent`. Everything else comes
  from Maven Central, except the Gradle plugins `kotlin-dsl`, `kotlinx-benchmark-plugin` and
  `kotest-framework-plugin-gradle`, which only the Gradle Plugin Portal has, and Node.js and Yarn, which come from
  `nodejs.org` and the GitHub releases of Yarn. `gradle/verification-metadata.xml` names the Node.js archive of every
  platform the pipeline runs on. Nothing is ever resolved from `mavenLocal()`.
- After a dependency changes, refresh the locks and the verification metadata and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --no-configuration-cache
  ```

  ```bash
  ./gradlew --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache check
  ```

- Kore builds only with released versions of Kreate. `gradle/verification-metadata.xml` trusts `com.davils` artifacts
  only by the signature of the Davils release key `36F578E8AD2D4391E0FA6C18154C625FAD370063`; a Kreate snapshot is
  never put into the catalog, and no trust rule for unsigned snapshots is ever added.
- A finding of the assurance gate that is a known, harmless property of a dependency gets an `ignore` rule in
  `build-logic/src/main/kotlin/kore.assurance.gradle.kts`, with a reason, the exact module and an expiry date no more
  than six months ahead. CRITICAL and HIGH findings can never be ignored.

## Public API

The `api/*.api` dumps are the contract. `kreateApiCheck` fails on every unrecorded change and
`kreateApiSemverCheck` on a version that does not match it. A deliberate change is recorded with
`./gradlew kreateApiDump` and gets a `CHANGELOG.md` entry. A breaking change needs a major version and a commit
marked as breaking. Deprecate before removing: `@Deprecated` with a `ReplaceWith`, for at least one minor release.

`@DavilsInternal` API that another Davils library uses changes only in a major version of Kore. Such a library does
not choose the Kore it runs with: on the buildscript class path of a consumer, the conflict resolution of Gradle may
hand Kreate a newer Kore than the one it was built against, and a declaration that was removed or changed there fails
with `NoSuchMethodError` at runtime, although both builds were green. To all other code the marker still promises
nothing, as its KDoc says.

- **The dumps record internal API.** `@DavilsInternal` declarations stay in the dumps like every other public
  declaration; `DavilsInternal` is never added to `nonPublicMarkers`, `ignoredPackages` or `ignoredClasses`. So
  `kreateApiSemverCheck` asks for a major version and a breaking commit as soon as one of them is removed or its
  signature changes.
- **Behaviour counts too.** A change of documented behaviour behind an unchanged signature, such as other code points
  escaped by `SafeText` or a leading `+` accepted by `parseIntOrNull`, waits for a major version as well. No dump shows
  it, so the tests in the table pin it: an assertion in them that changes or disappears is such a change, a new one
  is not.
- **The table stays current.** When a Davils library starts to use another `@DavilsInternal` declaration, it is added
  here with the release of that library and the test that pins it, before the next release of Kore.

  | Library | Built against | `@DavilsInternal` API it uses | Pinned by |
  |---|---|---|---|
  | Kreate 5.3.0 | `kore-core` 2.1.1 | `SafeText.text`, `SafeText.quoted`, `SafeText.isSafe` | `SafeTextTest` |
  | Kreate 5.3.0 | `kore-core` 2.1.1 | `parseIntOrNull`, `parseLongOrNull` | `IntegersTest` |
  | Kreate 5.3.0 | `kore-core` 2.1.1 | `parseDecimalOrNull` | `DecimalsTest` |
  | konfetti 1.0.0 | `kore-core` 2.3.1 | `SafeText.text`, `SafeText.REDACTED` | `SafeTextTest` |
  | konfetti 1.0.0 | `kore-core` 2.3.1 | `OrderedMap` | `OrderedMapTest` |
  | krypt 1.0.0 | `kore-core` 2.3.2 | `SafeText.text`, `SafeText.REDACTED` | `SafeTextTest` |
  | krypt 1.0.0 | `kore-core` 2.3.2 | `immutableSet` | `ImmutablesTest` |
  | krypt 1.0.0 | `kore-core` 2.3.2 | `OrderedMap`, `OrderedSet` | `OrderedMapTest`, `OrderedSetTest` |
  | kaml 1.0.0 | `kore-core` 2.3.1 | `SafeText.text`, `OrderedIndex`, `OrderedMap`, `OrderedSet`, `WorkBudget` | `SafeTextTest`, `OrderedIndexTest`, `OrderedMapTest`, `OrderedSetTest`, `WorkBudgetTest` |
  | kontinuity 1.0.0 | `kore-core` 2.3.2 | `SafeText.text`, `SafeText.quoted`, `immutableList`, `immutableMap`, `blocking` | `SafeTextTest`, `ImmutablesTest`, `BlockingTest` |
  | Konnect 1.0.0 | `kore-core` 2.3.2 | `NativeLibrary`, `NativeLoader`, `NativeResult`, `blocking` | `NativeLibraryTest`, `NativeResultTest`, `BlockingTest` |
  | konfetti 1.1.0 | `kore-core` 2.5.0 | `guard`, `grownDelay`, `fullJitter` | `GuardTest`, `GrowthTest`, `SpreadsTest` |
  | kollect 1.1.0 | `kore-core` 2.5.0 | `guard`, `grownDelay`, `fullJitter` | `GuardTest`, `GrowthTest`, `SpreadsTest` |
  | kontinuity 1.1.0 | `kore-core` 2.5.0 | `guard`, `grownDelay`, `fullJitter`, `equalJitter`, `proportionalJitter`, `decorrelatedJitter` | `GuardTest`, `GrowthTest`, `SpreadsTest` |
  | Knot 1.2.0 | `kore-core` 2.5.0 | `guard` | `GuardTest` |
  | kontext 1.1.0 | `kore-core` 2.5.0 | `OrderedMap`, `SafeText.quoted` | `OrderedMapTest`, `SafeTextTest` |

  Before every release, read the diff of these tests against the last release tag; it shows no changed or removed
  assertion:

  ```bash
  git diff "$(git describe --tags --abbrev=0)" -- '*SafeTextTest.kt' '*IntegersTest.kt' '*DecimalsTest.kt' \
    '*OrderedMapTest.kt' '*ImmutablesTest.kt' '*OrderedSetTest.kt' '*OrderedIndexTest.kt' '*WorkBudgetTest.kt' \
    '*BlockingTest.kt' '*NativeLibraryTest.kt' '*NativeResultTest.kt' '*GuardTest.kt' '*GrowthTest.kt' '*SpreadsTest.kt'
  ```

## Releases

The version lives in `gradle.properties` and nowhere else; `kreateReleaseGuard`, which runs before every publication,
refuses a build whose resolved version differs from it, so no environment variable can change what is published. Every build without a release tag is a
`-SNAPSHOT`. A release is a tag without a `v`, such as `1.0.0`, that equals that version, sits on a commit of `main`
and is built without any snapshot tooling; the guard refuses every other combination before anything is published.
Snapshots go to `https://repo.davils.com/snapshots`, releases to `https://repo.davils.com/releases`, and never to
Maven Central. The pipeline publishes with
`publishAllPublicationsToDavilsRepository` and `--no-configuration-cache`, with the credentials in `DAVILS_MAVEN_NAME`
and `DAVILS_MAVEN_SECRET`, as kotlin-workflow hands them to Gradle, and the signing key in `ORG_GRADLE_PROJECT_signingInMemoryKey`, `…KeyId` and `…KeyPassword`;
a release without a signing key fails.

## Git

Work on `develop`, release from `main`. Conventional Commits in English with the module as scope, such as
`feat(core): ...` or `fix(testing): ...`. Small commits, one concern each. Commit only when asked. No
`Co-Authored-By` trailer and no other attribution to tools or AI assistants.
