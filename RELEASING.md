# Releasing

The full runbook — secrets, signing key, the Central Portal flow, the order the
four repositories release in — lives in **`lightbatis/RELEASING.md`**. This file
covers only what is specific to this repository.

## Three modules, one bundle

`lightbatis-spring`, `lightbatis-spring-boot-autoconfigure` and
`lightbatis-spring-boot-starter` are published; `lightbatis-spring-sample` is
not — it exists to prove the other three work inside a real Boot context on H2,
and publishing it would put H2 into a consumer's dependency graph.

All three go up in a single deployment bundle, so either the whole release lands
or none of it does. That matters more here than elsewhere: the starter depends on
the other two, and a half-published release would resolve to a broken graph.

The starter has no classes at all — `module-info.java` is its only source file —
so `javadoc` fails outright on "No public or protected classes found to
document". Central still requires a javadoc artifact, so its `javadocJar` carries
a short note saying why it is empty. That is deliberate, not a workaround left
half-finished.

## `lightbatisVersion`

`gradle.properties` carries the core version these modules compile and depend
against. The release workflow refuses to run while it still reads `-SNAPSHOT`: a
starter that depends on a snapshot fails in someone else's build days later.

Release `lightbatis` first, then set:

```properties
version=0.1.0
lightbatisVersion=0.1.0
```

During local development `settings.gradle.kts` includes `../lightbatis` when that
directory exists, and Gradle substitutes the projects — so this property is not
resolved at all until CI, where the directory is absent. Between releases CI
resolves it from the Portal's snapshot repository, which
`build.gradle.kts` adds narrowed to the `io.github.lightbatis` group and to
snapshots only.

## Rehearse first

```bash
gh workflow run release.yml -f version=0.1.0 -f dry-run=true
```

Builds, tests, signs and assembles the bundle — and uploads nothing.
