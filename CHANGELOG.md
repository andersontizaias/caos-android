# Changelog

All notable changes to this project will be documented in this file.
Format: [Semantic Versioning](https://semver.org/).

---

## [1.0.0] — 2026-08-19

Primeira versão — port completo em Kotlin/Jetpack Compose de
[Caos](https://github.com/andersontizaias/Caos) v1.0.0 (Swift). Nasce direto em schema YAML v1,
sem legado de v0 pra portar.

### Added

- **`caos-core`** — módulo Kotlin puro, sem dependências de Android ou de terceiros
  - `CaosYamlParser` — parser YAML v1 recursivo hand-rolled, port fiel do `YAMLParser` do Swift
  - `CaosError`/`CaosParseException`, `CaosProps`, `CaosSchema`, `CaosScreen`/`CaosContainer`/
    `CaosEdgeInsets`, `CaosShard` — mesma API pública do Swift, mesma semântica
  - 60 testes, 98.5% de cobertura de linha
- **`caos-compose`** — módulo Android library com o engine de renderização
  - `CaosStore` — registry de shards (`@Composable` lambdas), providers síncronos e `StateFlow`
    no lugar do Combine do Swift
  - `LocalCaosStore`/`LocalCaosTapAction` — `CompositionLocal`s equivalentes aos `@Environment`
    do SwiftUI
  - `CaosScreenView` — carrega YAML dos assets, 4 estados (loading/erro/vazio/renderizado)
  - `CaosContainerView` — `LazyColumn`/`LazyRow`/`LazyVerticalGrid`
  - `CaosUnknownShardView` — gated por `BuildConfig.DEBUG`
  - `Modifier.caosShimmer(isActive)` — mesma curva de animação do `ShimmerModifier` do Swift
  - 26 testes (debug) / 26 testes com 1 skip esperado (release), 93.5% de cobertura de linha
- **`caos-lint`** — CLI JVM de validação de YAML, reusa `caos-core`
  - Mesmas regras e mensagens de saída do `caos-lint` Swift (`✓`/`⚠`/`✗`/`✅`)
  - Distribuído como fat jar standalone (`com.gradleup.shadow`), sem precisar do classpath do
    Gradle: `java -jar caos-lint-1.0.0-all.jar arquivo.yaml`
  - 12 testes, 94% de cobertura de linha
- **`caos-sample`** — app de exemplo reproduzindo o Quick Start do README Swift (`BalanceCard` +
  `home.yaml`), validado rodando de verdade num emulador
- **GitHub Actions** — workflows `lint.yml` (ktlint + Spotless + detekt), `ci.yml` (`./gradlew
  check` — testes, Robolectric, cobertura ≥90% via Kover, build do sample, smoke test cruzado
  `caos-lint` × `caos-sample`), `release.yml` (Maven Central via `com.vanniktech.maven.publish` +
  GitHub Release com o fat jar do `caos-lint` e o APK do `caos-sample`)
- **`version.txt`** — fonte única de verdade da versão, mesmo padrão do repo Swift

### Simplificações deliberadas em relação ao Swift

Documentadas com o raciocínio completo em [`PLAN_ANDROID.md`](./PLAN_ANDROID.md):

- **Sem `CaosSwiftUIView`/reflection** — em Compose, uma função `@Composable` já é a unidade de
  composição; registro de shard é sempre por lambda, sem precisar de um protocolo com
  `init(props:)` nem de type erasure (`AnyView`)
- **Sem `ScrollView` explícito** — `LazyColumn`/`LazyRow`/`LazyVerticalGrid` já são o próprio
  container de scroll, ao contrário de `LazyVStack`/`HStack`/`VGrid` no SwiftUI

### Known gaps

- Publicação real no Maven Central pendente de secrets (conta no Central Portal, namespace
  verificado, chave GPG) — o workflow está pronto, só falta a configuração que cabe ao usuário
- `release-please` não foi adicionado (exigiria instalar um GitHub App no repo)
