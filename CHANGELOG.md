# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

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
