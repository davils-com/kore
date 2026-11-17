# AGENT.md

Binding rules for every agent and every person working in this repository. Kore is the foundation library of
Davils: small, strict building blocks for Kotlin on the JVM that other Davils libraries and applications build on.
It is published under the Apache License 2.0 and is built to the standard it is used for, with security first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `kore-core` | `com.davils:kore-core` | Versions and ranges, UUIDs, validation, text positions and snippets, command line and environment parameters, principals, data sizes, the `Davils*` annotations and the architecture contracts |
| `kore-testing` | `com.davils:kore-testing` | Test support for code built on Kore: deterministic UUIDs and marked text; never on a production class path |
| `build-logic` | - | The convention plugins `kore.base`, `kore.kotlin-library` and `kore.root`; they hold nothing but `plugins { }` and `kreate { }` |

The root package of a module is `com.davils.kore.<module without kore->`, such as `com.davils.kore.core`, and its
Java module has the same name. `kore-core` declares it in `src/main/java/module-info.java` and exports only packages
with public API; every other module names it as `Automatic-Module-Name`.

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
- **No `!!`, no `lateinit`.** Model absence as a type or return early.
- **Explicit API.** Everything is `internal` unless a consumer needs it.
- **Names.** Public top-level types start with `Kore`, annotations shared by every Davils library with `Davils`
  (`DavilsInternal`, `DavilsExperimental`, `DavilsDsl`, `DavilsGenerated`, `DavilsPlatformBinding`). Functions,
  properties and non-public types carry no prefix.
- **`@DavilsInternal`** marks API shared between Davils libraries that consumers must not use; it fails the build of
  every caller that does not opt in. **`@DavilsExperimental`** marks API that may still change.
- **Immutable values.** Public types are immutable; public collections are built with `immutableList()`,
  `buildList` and the like, never stored from `toList()` or `+`, so a cast cannot change them.
- **No public `data class`.** It freezes `copy` and `componentN` into the binary API. Write a regular class with
  `equals`, `hashCode` and `toString`; `value class` stays allowed.
- **Explicit return types** on public functions with an expression body or a result; a block body that returns
  nothing omits `: Unit`, which the compiler rejects as redundant.
- **Kotlin first**, Java only where Kotlin offers no equivalent. Atomics come from `java.util.concurrent.atomic`.
  Experimental Kotlin API is allowed only through a local `@OptIn` and never appears in a public signature.
- **Nothing global.** No mutable singletons, no static configuration, no threads or timers of the library's own. The
  one sanctioned exception is the process-wide monotonic state of `KoreUuidV7Generator`, documented in its KDoc.
  Detekt does not enforce this rule; reviews do.

## Security rules

Every input of Kore may come from an attacker: arguments, the environment, version strings, UUIDs, text, paths and
the attributes of principals. Kore is built so that a mistake of a caller fails closed.

- **Limits before work.** Every entry point checks its input against `KoreLimits` before it does work that grows
  with the input, and refuses what is too large with a violation or an exception. Nothing is truncated silently.
  A new limit goes into `KoreLimits`, never into a private constant.
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
- **Secure randomness only.** No `java.util.Random`, `kotlin.random.Random` or `ThreadLocalRandom`.
- **Every sink has an injection test.** A renderer is tested with the `hostileText` fixture and
  `shouldBeSafeToShow`; a sensitive value has a property test that it appears nowhere.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library; fakes come from `kore-testing` or a `fixture`.
- One `context` per behaviour or function; test names read as sentences.
- Tests live in `src/unitTest/kotlin`, in the subpackage of the code they test. A fixture lives in the `fixture`
  folder of the only subpackage that uses it, or in the shared `fixture` folder of the area. `src/test` is rejected
  by the build.
- Property tests with adversarial generators for every parser and every security invariant.
- Every change ships with its tests; a fixed weakness ships with a test that would have caught it.

## Build

- Every module applies exactly one plugin, `id("kore.kotlin-library")`, and declares nothing but its description
  and its dependencies. Detekt, Kover, Kotest, Trivy, the compiler and publishing are configured only in
  `build-logic`, through Kreate.
- `build-logic` holds no Kotlin classes, no tasks of its own and no Gradle API calls beyond reading the version
  catalog. What Kreate cannot express yet is added to Kreate, never programmed here.
- Dependencies are declared in `gradle/libs.versions.toml`. Libraries of JetBrains may be added when they are
  needed; every other dependency needs the approval of a maintainer.
- Only `repo.davils.com` serves `com.davils` and its subgroups, through `exclusiveContent`. Everything else comes
  from Maven Central, except the Gradle plugins `kotlin-dsl` and `kotlinx-benchmark-plugin`, which only the Gradle
  Plugin Portal has. Nothing is ever resolved from `mavenLocal()`.
- After a dependency changes, refresh the locks and the verification metadata and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --no-configuration-cache
  ```

  ```bash
  ./gradlew --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache check
  ```

- While Kreate is a snapshot, `gradle/verification-metadata.xml` trusts `com.davils` artifacts whose version ends in
  `-SNAPSHOT`, because they change with every push and are not signed, and the pipeline runs Gradle with
  `--refresh-dependencies` so it never builds with a snapshot the cache kept. Once the release is out, raise the
  version in the catalog, remove that trust rule and the flag in `.github/kotlin-workflow.yml`, and record its
  signature.
- A finding of the assurance gate that is a known, harmless property of a dependency gets an `ignore` rule in
  `build-logic/src/main/kotlin/kore.base.gradle.kts`, with a reason, the exact module and an expiry date no more
  than six months ahead. CRITICAL and HIGH findings can never be ignored.

## Public API

The `api/*.api` dumps are the contract. `kreateApiCheck` fails on every unrecorded change and
`kreateApiSemverCheck` on a version that does not match it. A deliberate change is recorded with
`./gradlew kreateApiDump` and gets a `CHANGELOG.md` entry. A breaking change needs a major version and a commit
marked as breaking. Deprecate before removing: `@Deprecated` with a `ReplaceWith`, for at least one minor release.

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
