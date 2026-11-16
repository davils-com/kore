# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |
| < 1.0   | No        |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/kore/security/advisories/new),
or by email to development@davils.com.

Please include:

- the affected Kore version, module and JDK,
- a description of the impact — what an attacker gains,
- the steps to reproduce it, ideally as a minimal Kotlin snippet or test,
- any mitigation you are already aware of.

We acknowledge a report within three working days and agree on a disclosure date with you.

## Scope

Kore is a library: it runs with the privileges of the application that uses it. It treats everything it reads
from outside the program as untrusted: command line arguments, environment variables, values of parameter
sources, version strings, UUIDs, text, file paths and the attributes of principals.

The following are **in scope**:

- input that makes Kore use unbounded memory or CPU, overflow, or crash with an unexpected exception,
- untrusted text that reaches a message, a report, a `toString` or a usage text unescaped, so it can forge log
  lines or control a terminal,
- a value declared as sensitive that appears anywhere outside the value itself,
- a check that passes when it should refuse: an authorization helper of `KorePrincipal`, a path requirement, a
  version range or a converter that accepts what it documents as refused,
- a weakness in the generation of UUIDs.

The following are **not** vulnerabilities in Kore:

- a vulnerability in the JDK, the Kotlin standard library or `kotlinx.coroutines` — report those to the
  respective project,
- an application that passes a secret to a parameter it did not declare as sensitive, or logs a value it read,
- the time between a path check of a converter and the moment the application opens the file; those checks are
  documented as advisory,
- `kore-testing` on a production class path; its generators are deterministic by design.

## Known Limitations

- Kore has no type that holds a secret. A sensitive parameter returns its plain value; the application keeps it
  out of its own logs.
- A UUID of version 7 reveals the time it was created and carries fewer random bits than one of version 4. Never
  use a UUID as a capability or a token.
- `@DavilsInternal` is a compile-time opt-in, not an access boundary. Kotlin `internal` is public in JVM bytecode:
  Java code and reflection reach internal declarations through their mangled names. On the module path the
  `module-info` of `kore-core` exports only packages with public API, and implementation details live in packages
  it does not export; on the class path every class is reachable. Kore therefore validates in every constructor
  and keeps no secret in any field, internal ones included.
- Escapes are written as `\u{XXXX}`, `\n`, `\r` and `\t`. A literal backslash in free text is shown as it is, so
  input can display text that looks like an escape, but never a real control character. Right-to-left letters are
  shown as they are; a viewer may reorder the neutral characters around them.

## Verifying a Release

Releases are published to `https://repo.davils.com/releases` and signed with the release key of Davils; its
fingerprint is published in this section with the first release, so a consumer can pin it as a `trusted-key` in its
own `gradle/verification-metadata.xml`. A release also carries a signed build provenance attestation made by the
GitHub Actions run that built it:

```bash
gh attestation verify kore-core-1.0.0.jar --repo davils-com/kore --signer-repo davils-com/kotlin-workflow
```

Snapshots in `https://repo.davils.com/snapshots` are neither signed nor attested and are not meant for production.
