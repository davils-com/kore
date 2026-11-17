# Third-Party Material

No published artifact of Kore contains third-party code. The tests use the following material.

| Material | Used in | Source | License |
|---|---|---|---|
| The regular expression for a valid version and the examples of precedence of *Semantic Versioning 2.0.0* by Tom Preston-Werner | `kore-core/src/unitTest/kotlin/com/davils/kore/core/version/SemanticVersionConformanceTest.kt`, `SemanticVersionTest.kt` | [semver.org](https://semver.org) | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) |
| The tables of caret, tilde, X-range, primitive and hyphen ranges of node-semver | `kore-core/src/unitTest/kotlin/com/davils/kore/core/version/NpmRangeConformanceTest.kt` | [npm/node-semver](https://github.com/npm/node-semver) | ISC, see below |

The build and the tests depend on libraries under their own licenses; `gradle/verification-metadata.xml` records
each of them, and the Trivy license scan of `./gradlew check` checks them.

## node-semver

```
The ISC License

Copyright (c) Isaac Z. Schlueter and Contributors

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
```
