# Caos Android — Implementation Plan (Jetpack Compose)

> **Status:** Phases 0–6 complete — API parity with Swift v1.0.0 achieved
> (`caos-core`, `caos-compose`, `caos-lint`, `caos-sample`, CI/CD, docs). Actual publishing to
> Maven Central is pending secrets only the user can configure — see Phase 5.
> **Source of truth:** [`andersontizaias/Caos`](https://github.com/andersontizaias/Caos) v1.0.0 (SwiftUI, `Sources/Caos/`)
> **This document replaces** the earlier draft at `chaos/PLAN_ANDROID.md`, which assumed an
> architecture (`NSClassFromString`, `kaml` dependency) that doesn't match the real Caos iOS
> implementation. This plan was written by reading the current Swift source code, not a
> hypothetical version of it.

## Overview

Caos (**C**onfigurable **A**utomated **O**n-demand **S**creens) is a Server-Driven UI framework:
it generates Compose screens dynamically from YAML files, with no app redeploy needed. Android
mirrors the iOS architecture 1:1 wherever the two frameworks are equivalent, and simplifies
wherever Compose natively solves what SwiftUI had to work around (type erasure, the
`init(props:)` protocol).

The same `caos.yaml` file works on both platforms without modification — both parsers are
hand-rolled, stdlib-only, and must agree byte-for-byte on the resulting tree.

---

## Why the earlier plan was wrong

The draft at `chaos/PLAN_ANDROID.md` (an old repo, no git, likely from a UIKit prototyping
phase) described:

- iOS resolving shards via `NSClassFromString` (runtime reflection) → **doesn't exist** in the
  current code. `CaosStore.register(type:view:)` is explicit closure-based registration,
  resolved at compile time.
- A YAML parser via a `kaml` (Kotlin Multiplatform) dependency → the real Swift parser is
  **hand-rolled, zero third-party dependencies** (`YAMLParser` in `CaosParser.swift`, ~250
  lines, a recursive mapping/sequence/scalar parser). Using `kaml` on Android would break the
  "zero dependencies" parity that's an explicit design value of the project (see the README:
  *"No third-party dependencies — Parser is stdlib-only"*).

Since iOS already uses explicit registration (not reflection), the "problem" the old draft tried
to solve with `CaosRegistry` doesn't exist — Android just needs the direct equivalent.

---

## API Mapping (Swift → Kotlin)

| Swift (`Sources/Caos/`) | Kotlin (`caos-android`) | Notes |
|---|---|---|
| `CaosError` (enum) | `sealed class CaosError` | `MissingVersion`, `InvalidYaml(line, reason)`, `UnsupportedVersion(version)` |
| `CaosProps` (struct over `[String: Any]`) | `data class CaosProps(val data: Map<String, Any?>)` | `string/int/double/bool/nested/array/hexColor` — same semantics, including a non-optional `double()` defaulting to `0.0` and `hexColor()` validating `#RGB`/`#RRGGBB`/`#AARRGGBB` without converting to `Color` |
| `CaosSchema` | `data class CaosSchema(val version: Int, val screens: List<CaosScreen>)` | |
| `CaosContainer` / `CaosEdgeInsets` | equivalent data classes | `type: String` stays a free-form string (`vertical`\|`horizontal`\|`grid`), not an enum — keeps schema parity |
| `CaosShard` | `data class CaosShard(val type: String, val id: String = "", val props: CaosProps = CaosProps())` | |
| `CaosScreen` (a mutable class in Swift) | `data class CaosScreen(val id: String, val containerConfig: CaosContainer, val shardList: List<CaosShard>)` | Kotlin doesn't need the mutability Swift uses internally during parsing — builds the immutable instance directly |
| `YAMLParser` (hand-rolled recursive parser) | `internal object CaosYamlParser` | line-by-line port of `parseMapping`/`parseSequence`/`parseScalar`/`findKeyColon`/`stripInlineComment`/`leadingSpaces`. **No external dependency** |
| `CaosParser.parse(_:)` | `object CaosParser { fun parse(content: String): CaosSchema }` | throws `CaosParseException(val error: CaosError)` instead of `throws` |
| Deprecated v0 API (`CaosParser(content:)`, `getScreens()`, `parseLegacy`) | **not ported** | v0→v1 legacy compat in Swift; Android is born directly at v1 |
| `CaosStore` (`ObservableObject` + Combine) | `class CaosStore` | `shardRegistry: MutableMap<String, @Composable (CaosProps) -> Unit>`; `providers: MutableMap<String, () -> Any?>`; `subjects: MutableMap<String, MutableStateFlow<Any?>>` instead of `CurrentValueSubject` |
| `store.register(type:factory:)` (closure) | `store.register(type: String, content: @Composable (CaosProps) -> Unit)` | single canonical form — see next row |
| `store.register<V: CaosSwiftUIView>(type:view:)` (generic + `init(props:)`) | **eliminated, no direct substitute** | in Compose a `@Composable` function is already the unit of composition; no need for a `CaosSwiftUIView`-style protocol or reflection to instantiate. Every shard registers as a lambda — a real simplification, not a loss of functionality |
| `store.view(for:props:)` → `AnyView` | `@Composable fun CaosStore.Render(type: String, props: CaosProps)` | no type erasure — Compose doesn't need `AnyView` |
| `store.register(key:provider:)` | identical, `() -> Any?` | |
| `store.register(key:publisher:)` (Combine `AnyPublisher`) | `store.register(key: String, flow: StateFlow<Any?>)` | |
| `store.resolve<T>(key:)` | `inline fun <reified T> resolve(key: String): T?` | |
| `store.publisher<T>(for:)` → `AnyPublisher<T, Never>` | `fun <T> flowFor(key: String): StateFlow<T?>?` | consumed via `collectAsState()`, no `for await` wrapper needed |
| `CaosEnvironment` (`@Environment(\.caosStore)`, `.caosStore(_:)`) | `val LocalCaosStore = staticCompositionLocalOf<CaosStore> { ... }` + `CompositionLocalProvider` | |
| `CaosTapHandler` (`@Environment(\.caosTapAction)`, `.onCaosTap { }`) | `val LocalCaosTapAction = staticCompositionLocalOf<(String, Map<String, Any?>) -> Unit> { { _, _ -> } }` | |
| `CaosSwiftUIView` (protocol) | **eliminated** | see the `register<V>` row above |
| `CaosScreenView` (`View` + `.task` + `Bundle.path`) | `@Composable fun CaosScreenView(name: String, modifier: Modifier = Modifier)` | loads via `context.assets.open("$name.yaml")` in `LaunchedEffect(name)`; same 4 states: loading (`CircularProgressIndicator`) / error (formatted `CaosError`) / empty (`"No screen found..."`) / rendered |
| `CaosContainerView` (`ScrollView` + `LazyVStack`/`LazyHStack`/`LazyVGrid`) | `@Composable fun CaosContainerView(screen: CaosScreen)` | `Column`+`verticalScroll` / `Row`+`horizontalScroll` / `LazyVerticalGrid(columns = GridCells.Fixed(2))` based on `container.type`; identical padding and spacing |
| `CaosUnknownShardView` (`#if DEBUG`) | `@Composable fun CaosUnknownShardView(type: String)` | `if (BuildConfig.DEBUG) { Text/Box with a warning } else { }` |
| `ShimmerModifier` / `.shimmer(isActive:)` | `fun Modifier.caosShimmer(isActive: Boolean): Modifier` | `rememberInfiniteTransition` + `Brush.linearGradient`, same curve (linear, 1.4s, repeat forever, no autoreverse) |
| `caos-lint` (SPM executable) | `caos-lint` module (`application` plugin, depends on `caos-core`) | same rules (shard missing `type`, duplicate `id`, warning for a shard with no `id`) and same output format (`✓`/`⚠`/`✗`) — output should be directly comparable to the Swift CLI |

---

## Repository Structure

```
caos-android/
├── caos-core/            # pure Kotlin module (JVM, no Android/Compose plugin)
│   ├── src/main/kotlin/io/github/andersontizaias/caos/core/
│   │   ├── CaosError.kt
│   │   ├── CaosProps.kt
│   │   ├── CaosSchema.kt
│   │   ├── CaosScreen.kt   (+ CaosContainer, CaosEdgeInsets)
│   │   ├── CaosShard.kt
│   │   ├── CaosParser.kt
│   │   └── CaosYamlParser.kt   (internal)
│   └── src/test/kotlin/.../Fixtures/   ← literal copy of the 3 fixtures from the Swift repo
├── caos-compose/          # Android library module, applies Compose
│   ├── src/main/kotlin/io/github/andersontizaias/caos/compose/
│   │   ├── CaosStore.kt
│   │   ├── CaosEnvironment.kt      (LocalCaosStore)
│   │   ├── CaosTapHandler.kt       (LocalCaosTapAction)
│   │   ├── CaosScreenView.kt
│   │   ├── CaosContainerView.kt
│   │   ├── CaosUnknownShardView.kt
│   │   └── CaosShimmer.kt
│   └── src/test/kotlin/...         ← Robolectric + Roborazzi
├── caos-lint/              # JVM CLI module (application plugin)
├── caos-sample/            # example Compose app (BalanceCard + home.yaml, same as the Swift README)
├── Docs/                   # mirrors the Swift repo's Docs/
├── .github/workflows/      # ci.yml, lint.yml, release.yml, nightly.yml
├── build.gradle.kts / settings.gradle.kts
├── README.md / CHANGELOG.md
└── LICENSE (MIT)
```

### Build coordinates

- **minSdk:** 24 · **compileSdk / targetSdk:** 36 — aligned with `little_bank_android`, the
  framework's first expected consumer.
- **Compose BOM:** the same version already used by `little_bank_android`
  (`androidx.compose:compose-bom:2026.05.00`), to avoid version conflicts when the app consumes
  the framework as a local/composite build dependency.
- **Kotlin:** JVM target 21 (same as the app).

---

## Phases

### Phase 0 — Repository bootstrap
`settings.gradle.kts` with the 4 modules, ktlint + detekt + spotless configured following the
same conventions as `little_bank_android/app/build.gradle.kts`, CI skeleton, README stub,
`LICENSE` MIT.

### Phase 1 — `caos-core` ✅
Port of the hand-rolled YAML parser + schema models. **Central acceptance criterion:** copy the
3 fixtures from the Swift repo (`valid_v1.yaml`, `invalid_no_version.yaml`, `edge_cases.yaml`)
into `caos-core/src/test/.../Fixtures/` and write tests that compare the Kotlin-parsed tree
against the Swift-parsed tree (same values, same types) — not just "doesn't crash."

- [x] `CaosParser.parse()` reads the same iOS YAML v1 with no changes to the file
- [x] `CaosParseException` carries a `CaosError` with line/reason, like Swift
- [x] `CaosProps.nested()` works for hierarchical `padding`
- [x] Module compiles without `android.jar` (pure JVM)
- [x] Coverage ≥ 90% (98.5%, 60 tests — [PR #1](https://github.com/andersontizaias/caos-android/pull/1))

### Phase 2 — `caos-compose` ✅
`CaosStore`, `LocalCaosStore`/`LocalCaosTapAction`, `CaosScreenView`, `CaosContainerView`,
`CaosUnknownShardView`, `Modifier.caosShimmer`.

- [x] `CaosScreenView` correctly renders registered shards, across all 3 container types
- [x] A shard with `dataKey` updates the UI automatically when the `StateFlow` emits
- [x] An unregistered shard shows `CaosUnknownShardView` without crashing, hidden in release
- [x] Tests run via **Robolectric** (Roborazzi is on the classpath for future screenshot tests;
      current tests use `compose-ui-test` assertions, no emulator —
      [PR #2](https://github.com/andersontizaias/caos-android/pull/2))
- [x] Coverage ≥ 90% (93.5%, 26 debug tests / 26 release tests with 1 expected skip due to `BuildConfig.DEBUG`)

### Phase 3 — `caos-lint` ✅
JVM CLI reusing `caos-core`. Text output line-for-line comparable to the Swift `caos-lint`, to
keep the installation docs (`swift run caos-lint` / `./gradlew :caos-lint:run --args=...`)
consistent across both READMEs. 94% coverage, 12 tests —
[PR #3](https://github.com/andersontizaias/caos-android/pull/3).

### Phase 4 — `caos-sample` ✅
Reproduces the Swift README's Quick Start: `home.yaml` with `BalanceCard`, `MainActivity`
registering the shard and a reactive key, exactly like the Swift `MyApp` example. Validated by
actually running it on an emulator (card rendered + tap dispatching `onTap`), not just compiled
— [PR #4](https://github.com/andersontizaias/caos-android/pull/4).

### Phase 5 — CI/CD and Distribution ✅ (real publishing pending credentials)
- [x] `lint.yml`: ktlint + Spotless + detekt on every push
- [x] `ci.yml`: `./gradlew check` (tests + Robolectric + ktlint + detekt + Spotless + Kover
      ≥90%, already chained via task dependencies in each module — confirmed with
      `./gradlew :<module>:check --dry-run`), plus building `caos-sample` and a smoke test that
      runs `caos-lint` against `caos-sample`'s `home.yaml` (cross-module dogfooding)
- [x] `release.yml`: publishes `caos-core` and `caos-compose` to Maven Central via
      `com.vanniktech.maven.publish` (`publishAndReleaseToMavenCentral`), coordinates
      `io.github.andersontizaias:caos-core` / `io.github.andersontizaias:caos-compose`; also
      builds `caos-lint`'s fat jar (`com.gradleup.shadow` plugin, version 8.3.x — the 9.x line
      requires a newer Gradle API than the 8.14.1 used here) and `caos-sample`'s debug APK,
      both attached to a GitHub Release
  - **Pending on the user's side, not something I can implement:** the workflow only actually
    publishes once the repo has the `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`,
    `GPG_SIGNING_KEY`, `GPG_SIGNING_PASSWORD` secrets configured — which requires creating a
    [Central Portal](https://central.sonatype.com) account, verifying the
    `io.github.andersontizaias` namespace, and generating a GPG key. None of this was created or
    simulated; the workflow is ready to use as soon as those secrets exist.
- [ ] `release-please` for automated changelogs — **a conscious decision not to implement this
      now**: it requires installing the `release-please` GitHub App on the repo, an
      account-level action that's up to the user. `version.txt` (single source of truth, read
      by the root `build.gradle.kts`) is already in place as a prerequisite, in case it's added
      later.

### Phase 6 — Docs and parity certification ✅
`README.md` mirroring the Swift README's sections (Architecture, Requirements, Local
Development, Quick Start, YAML Schema Reference, CaosProps API, Registering Shards, Data
Binding, Tap Events, Loading States, YAML Validation, Installation), an iOS↔Android parity
table, `CHANGELOG.md` with a `v1.0.0` entry —
[PR #6](https://github.com/andersontizaias/caos-android/pull/6).

---

## iOS vs Android Parity Table

Moved to [`PARITY.md`](./PARITY.md) — single source of truth, also linked from `README.md`, so
the two tables don't drift apart as the project evolves past v1.0.0.

---

## Known Risks

1. **Porting the YAML parser** was the highest-risk/highest-effort item — a ~250-line recursive
   parser with subtle indentation, quoting, and inline-comment rules. Mitigation: port
   function-by-function in the same order as the Swift version, and validate against the shared
   fixtures before writing anything in `caos-compose`.
2. **`StateFlow` vs `Combine`**: `CurrentValueSubject` accepts `NSNull()` as a sentinel for "no
   value yet"; in Kotlin, use `MutableStateFlow<Any?>(null)` and check `providers` before
   `flows` in `resolve()`, replicating Swift's priority order (`providers` beats `subjects`).
3. **`BuildConfig.DEBUG`** requires the Android plugin applied on the `caos-compose` module (not
   on `caos-core`) — confirm `buildFeatures.buildConfig = true` is set, otherwise the field
   doesn't exist.
