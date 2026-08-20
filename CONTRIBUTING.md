# Contributing to Caos Android

Thank you for your interest in contributing! Please follow the guidelines below.

## Environment setup

```bash
git clone https://github.com/andersontizaias/caos-android.git
cd caos-android

# Builds every module
./gradlew build
```

Requires Android Studio or a JDK 21 (e.g. Android Studio's bundled JBR) plus the Android SDK
(`compileSdk`/`targetSdk` 36). See [Requirements](./README.md#requirements) in the README.

## Workflow

1. Fork the repository and create your branch from `main`:
   ```bash
   git checkout main
   git checkout -b feature/my-feature
   ```

2. Implement your change following the guidelines below

3. Make sure everything passes — this single command chains ktlint, Spotless, detekt, tests
   (JVM and Robolectric), and coverage verification (Kover, ≥90%) across every module:
   ```bash
   ./gradlew check
   ```

4. Open a Pull Request to `main` with a title following the Conventional Commits format

## Code standards

- **MV architecture**: no ViewModels. Composables read from `CaosStore` via
  `LocalCaosStore.current`
- **ktlint**: `./gradlew ktlintCheck` must pass with 0 violations
- **detekt**: `./gradlew detekt` must pass with 0 violations
- **API 24+**: don't use APIs exclusive to newer Android versions without a version guard
- **Stdlib only in `caos-core`**: `CaosParser`/`CaosYamlParser` must have no external
  dependencies — that parity with the Swift parser is a deliberate design choice, not an
  oversight

## Adding a new shard type

1. Create your `@Composable` function taking `CaosProps` — no protocol to conform to
2. Document the accepted `CaosProps` with types and default values
3. Register via `CaosStore.register(type = "...") { props -> ... }` in the consumer app
4. Add unit tests covering the props

## Adding support for new props

1. Accept the new field via `props.string()`, `props.double()`, etc.
2. Never silently fall back — use `?: defaultValue` explicitly
3. Update the README with the new prop in the reference table

## Tests

- Minimum coverage: **90%** per module (`caos-core`, `caos-compose`, `caos-lint`), enforced by
  Kover's `koverVerify`
- `caos-compose` tests run on Robolectric — no emulator required
- YAML fixtures shared with the Swift repo go in `caos-core/src/test/resources/fixtures/`

## Commits — Conventional Commits

Every commit and PR title **must** follow the
[Conventional Commits](https://www.conventionalcommits.org/) standard.

### Format

```
<type>(<optional scope>): <short description>

[optional body]

[BREAKING CHANGE: <description> — optional]
```

### Valid types

| Type | When to use |
|---|---|
| `feat` | New user-facing functionality |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Formatting, no behaviour change |
| `refactor` | Refactoring with no behaviour change |
| `test` | Adding or fixing tests |
| `chore` | CI, dependencies, configuration, build |
| `perf` | Performance improvement |
| `ci` | Changes specific to CI workflows |
| `build` | Build system, Gradle configuration |
| `revert` | Reverts a previous commit |

### Examples

```bash
feat: add horizontal grid container support
fix(parser): fix crash with YAML missing version field
docs: add reactive binding example to README
chore: bump compose-bom to 2026.06.00
feat!: drop CaosSwiftUIView-style shard registration   # breaking change
```

### Breaking changes

Add `!` after the type or include `BREAKING CHANGE:` in the commit body to signal that the major
version should be incremented.

## Code of conduct

Be respectful. Constructive criticism is welcome; personal attacks are not.
