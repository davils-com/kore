# Kore

Kore is the foundation library of Davils: small, strict building blocks for Kotlin on the JVM and on WebAssembly
(`wasmJs`) that are safe to feed with untrusted input. It is published under the [Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `kore-core` | `com.davils:kore-core` | Semantic versions and npm-style ranges, UUIDs of version 4 and 7, validation with paths, stable codes and common rules, text positions, edits, snippets, diagnostics, line breaks and decoding, rules for names, close matches, the containment of foreign code, `Outcome`, collections, data sizes, admission and single flight, the masked process environment, command line and environment parameters, principals, architecture contracts and the `Davils*` annotations |
| `kore-testing` | `com.davils:kore-testing` | Deterministic UUIDs, marked text, hostile text and clocks for tests; keep it out of production class paths |
| `kore-serialization` | `com.davils:kore-serialization` | Serializers of kotlinx-serialization for durations, data sizes, versions, UUIDs and violation codes, and `TextSerializer` for identifiers of your own |

Kore is a Kotlin Multiplatform library for two targets, `jvm` and `wasmJs`, and is written for Kotlin 2.4 or newer.
Besides the Kotlin standard library (2.4.21 or newer) `kore-core` has a single runtime dependency,
`kotlinx-coroutines-core`; `kore-serialization` adds `kotlinx-serialization-core`.
On the JVM it requires JDK 25, and `kore-core` is a named Java module, `com.davils.kore.core`. On Wasm it is tested
under Node.js and runs in the browser as well.

A few parts exist on the JVM only, because they are built on the JDK: the converters `Converters.path`,
`Converters.file` and `Converters.uri`, the conversion between `Uuid` and `java.util.UUID`, `blocking`,
`NativeLibrary` and `HostPlatform`. Everything else is common code and behaves the same on both targets.

## Installation

Kore is published to the Davils repository, not to Maven Central. Declare it for the `com.davils` group only, so no
other repository can ever serve a Davils artifact and no Davils repository is ever asked for anything else:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.davils:kore-core:2.3.0")
    implementation("com.davils:kore-serialization:2.3.0")   // only if you use kotlinx-serialization
    testImplementation("com.davils:kore-testing:2.3.0")
}
```

Gradle picks the JVM or the Wasm variant from these coordinates. A Maven build names the JVM artifacts directly, from
2.0.0 on `com.davils:kore-core-jvm` and `com.davils:kore-testing-jvm`, and from 2.1.1 on
`com.davils:kore-serialization-jvm`.

Snapshots of the `develop` branch are in `https://repo.davils.com/snapshots`. They are neither signed nor attested
and not meant for production.

## A short tour

### Parameters from the command line and the environment

```kotlin
class ServerParameters : Parameters(environmentPrefix = "APP") {
    val port: Int by required("--port", "-p", type = Converters.int) {
        describedAs("Port the server listens on")
    }
    val host: String by defaulted("--host", default = "0.0.0.0")
    val token: String? by optional("--token") { sensitive() }
    val tags: List<String> by multiple("--tag") { split(",") }
}

fun main(argv: Array<String>) {
    val parameters = ServerParameters()
    parameters.load(argv)
}
```

`load` reports every problem at once in a `ParameterException`; unknown arguments are an error. A sensitive
value never appears in a message, a report or `describe()`, and reading one from the command line, where other
users of the machine can see it, raises a warning. Every input is bounded by `ParameterLimits`.

### Versions and ranges

```kotlin
val range = SemanticVersionRange.parse("^1.2.0 || >=2.0.0-rc.1 <2.1.0")
val version = SemanticVersion.parse("1.4.2")

range.contains(version)                                       // true
range.contains(SemanticVersion.parse("1.5.0-beta.1"))         // false: pre-releases only on the same core
val newest = range.maxSatisfying(listOf("1.2.0", "1.9.3", "2.0.0").map(SemanticVersion::parse))   // 2.0.0
```

Membership always states its pre-release policy, `SameCoreOnly` by default. Parsing is linear, ASCII-only and bounded
by `Limits`.

### Validation

```kotlin
class Sink(val name: String, val queueSize: Int) : Validatable {
    override fun ValidationScope.validate() {
        if (name.isBlank()) fail(Sink::name, "must not be blank", ViolationCode.REQUIRED)
        if (queueSize <= 0) fail(Sink::queueSize, "must be positive")
    }
}

Sink(name = "", queueSize = 0).validateOrThrow("sink")
```

A `ValidationException` lists every violation with its path and stable code. Reports escape everything that came
from the input, so they are safe to log.

### Principals

```kotlin
val alice = principal("alice") {
    roles("operator")
    tenant = "acme"
}

withPrincipal(alice) {
    val caller = requirePrincipal()
    check(caller.hasAllRoles("operator"))
}
```

`hasAllRoles` needs at least one role, delegation stays within a tenant unless allowed explicitly, attributes are
sensitive by default and `withPrincipal` never replaces a different principal silently.

### Identities and sizes

```kotlin
val id = UuidV7.random()          // monotonic, SecureRandom, never a capability token
val limit = 64.mebibytes
```

### Foreign code, outcomes and names

```kotlin
val loaded = attempted { plugin.load() }          // a Result; passes on only the caller's own cancellation
val port: Outcome<Int, String> = readPort(text)   // Success or Failure, with a typed failure
val id = NameRule.QUALIFIED.requireValid(text, kind = "plugin id")
val hint = closestMatch("--prot", listOf("--port", "--host"))   // "--port"
```

`contained` and `attempted` replace `runCatching` around foreign code: they never swallow a cancellation or a failure
of the virtual machine. `NameRule` checks names in one linear pass without a regular expression.

### Sizes, rules and diagnostics

```kotlin
val limit = DataSize.parse("64 MiB")              // refuses "64 MB" rather than guessing what it means
class Upload(val timeout: Duration, val chunk: DataSize) : Validatable {
    override fun ValidationScope.validate() {
        positive(Upload::timeout, timeout)        // refuses zero, negative and infinite durations
        positive(Upload::chunk, chunk)
    }
}
val report = renderDiagnostics("settings.yaml", diagnostics, LineMap.of(text))
```

### Serialization

```kotlin
@Serializable
class Settings(@Contextual val timeout: Duration, @Contextual val limit: DataSize)

val json = Json { serializersModule = coreSerializers }
json.decodeFromString<Settings>("""{"timeout": "45s", "limit": "64 MiB"}""")
```

## Security

Every input of Kore may come from an attacker. Each entry point checks its input against `Limits` before it does
any work, escapes untrusted text wherever it is shown and fails closed. See [SECURITY.md](SECURITY.md) for the scope,
known limitations and how to report a vulnerability privately.

## Building

```bash
./gradlew check
```

It needs JDK 25 and network access: besides compilation with warnings as errors, Detekt, the tests and coverage it
runs Trivy and the Kreate assurance gate. See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md).

## License

Copyright 2026 Davils. Licensed under the [Apache License, Version 2.0](LICENSE).
