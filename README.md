# Kore

Kore is the foundation library of Davils: small, strict building blocks for Kotlin on the JVM that are safe to feed
with untrusted input. It is published under the [Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `kore-core` | `com.davils:kore-core` | Semantic versions and npm-style ranges, UUIDs of version 4 and 7, validation with paths and stable codes, text positions, edits and snippets, command line and environment parameters, principals, data sizes, architecture contracts and the `Davils*` annotations |
| `kore-testing` | `com.davils:kore-testing` | Deterministic UUIDs and marked text for tests; keep it out of production class paths |

Kore requires JDK 25 and is written for Kotlin 2.4 or newer. Besides the Kotlin standard library (2.4.21 or newer) it
has a single runtime dependency, `kotlinx-coroutines-core`. `kore-core` is a named Java module, `com.davils.kore.core`.

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
    implementation("com.davils:kore-core:1.0.1")
    testImplementation("com.davils:kore-testing:1.0.1")
}
```

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

## Security

Every input of Kore may come from an attacker. Each entry point checks its input against `Limits` before it does
any work, escapes untrusted text wherever it is shown and fails closed. See [SECURITY.md](SECURITY.md) for the scope,
known limitations and how to report a vulnerability privately.

## Building

```bash
./gradlew check
```

It needs JDK 25 and network access: besides compilation with warnings as errors, Detekt, the tests and coverage it
runs Trivy and the Kreate assurance gate. See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENT.md](AGENT.md).

## License

Copyright 2026 Davils. Licensed under the [Apache License, Version 2.0](LICENSE).
