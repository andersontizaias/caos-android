# Changelog

All notable changes to this project will be documented in this file.
Format: [Semantic Versioning](https://semver.org/).

---

## [1.0.0] — 2026-08-19

First release — a complete Kotlin/Jetpack Compose port of
[Caos](https://github.com/andersontizaias/Caos) v1.0.0 (Swift). Born directly at YAML schema v1,
with no v0 legacy to port.

### Added

- **`caos-core`** — pure Kotlin module, no Android or third-party dependencies
  - `CaosYamlParser` — hand-rolled recursive YAML v1 parser, a faithful port of Swift's `YAMLParser`
  - `CaosError`/`CaosParseException`, `CaosProps`, `CaosSchema`, `CaosScreen`/`CaosContainer`/
    `CaosEdgeInsets`, `CaosShard` — same public API as Swift, same semantics
  - 60 tests, 98.5% line coverage
- **`caos-compose`** — Android library module with the rendering engine
  - `CaosStore` — shard registry (`@Composable` lambdas), synchronous providers, and `StateFlow`
    in place of Swift's Combine
  - `LocalCaosStore`/`LocalCaosTapAction` — `CompositionLocal`s equivalent to SwiftUI's
    `@Environment`
  - `CaosScreenView` — loads YAML from assets, 4 states (loading/error/empty/rendered)
  - `CaosContainerView` — `LazyColumn`/`LazyRow`/`LazyVerticalGrid`
  - `CaosUnknownShardView` — gated by `BuildConfig.DEBUG`
  - `Modifier.caosShimmer(isActive)` — same animation curve as Swift's `ShimmerModifier`
  - 26 tests (debug) / 26 tests with 1 expected skip (release), 93.5% line coverage
- **`caos-lint`** — JVM CLI for YAML validation, reuses `caos-core`
  - Same rules and output messages as Swift's `caos-lint` (`✓`/`⚠`/`✗`/`✅`)
  - Distributed as a standalone fat jar (`com.gradleup.shadow`), no Gradle classpath needed:
    `java -jar caos-lint-1.0.0-all.jar file.yaml`
  - 12 tests, 94% line coverage
- **`caos-sample`** — example app reproducing the Swift README's Quick Start (`BalanceCard` +
  `home.yaml`), validated by actually running it on an emulator
- **GitHub Actions** — `lint.yml` workflow (ktlint + Spotless + detekt), `ci.yml` (`./gradlew
  check` — tests, Robolectric, ≥90% coverage via Kover, sample build, cross-module smoke test
  `caos-lint` × `caos-sample`), `release.yml` (Maven Central via `com.vanniktech.maven.publish` +
  GitHub Release with `caos-lint`'s fat jar and `caos-sample`'s APK)
- **`version.txt`** — single source of truth for the version, same pattern as the Swift repo

### Deliberate simplifications relative to Swift

Documented with the full reasoning in [`PLAN_ANDROID.md`](./PLAN_ANDROID.md):

- **No `CaosSwiftUIView`/reflection** — in Compose, a `@Composable` function is already the unit
  of composition; shard registration is always by lambda, with no need for an `init(props:)`
  protocol or type erasure (`AnyView`)
- **No explicit `ScrollView`** — `LazyColumn`/`LazyRow`/`LazyVerticalGrid` are already their own
  scroll container, unlike SwiftUI's `LazyVStack`/`HStack`/`VGrid`

### Known gaps

- Actual publishing to Maven Central is pending secrets (Central Portal account, verified
  namespace, GPG key) — the workflow is ready, only the configuration that's up to the user is
  missing
- `release-please` wasn't added (would require installing a GitHub App on the repo)
