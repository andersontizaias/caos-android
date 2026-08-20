# Caos Android — Plano de Implementação (Jetpack Compose)

> **Status:** Planejado
> **Fonte da verdade:** [`andersontizaias/Caos`](https://github.com/andersontizaias/Caos) v1.0.0 (SwiftUI, `Sources/Caos/`)
> **Este documento substitui** o rascunho anterior em `chaos/PLAN_ANDROID.md`, que presumia uma
> arquitetura (`NSClassFromString`, dependência `kaml`) que não corresponde à implementação real do
> Caos iOS. Este plano foi escrito lendo o código-fonte Swift atual, não uma versão hipotética dele.

## Visão Geral

Caos (**C**onfigurable **A**utomated **O**n-demand **S**creens) é um framework de Server-Driven UI:
gera telas Compose dinamicamente a partir de arquivos YAML, sem precisar de redeploy do app. O
Android replica a arquitetura do iOS 1:1 onde os frameworks se equivalem, e simplifica onde Compose
já resolve nativamente o que SwiftUI precisou contornar (type erasure, protocolo `init(props:)`).

O mesmo arquivo `caos.yaml` funciona em ambas as plataformas sem modificação — os dois parsers são
hand-rolled, stdlib-only, e devem concordar byte a byte na árvore resultante.

---

## Por que o plano anterior estava errado

O rascunho em `chaos/PLAN_ANDROID.md` (repo antigo, sem git, provavelmente de uma fase de
prototipagem UIKit) descrevia:

- iOS resolvendo shards via `NSClassFromString` (reflection em runtime) → **não existe** no código
  atual. `CaosStore.register(type:view:)` é registro explícito por closure, resolvido em tempo de
  compilação.
- Parser YAML via dependência `kaml` (Kotlin Multiplatform) → o parser Swift real é **hand-rolled,
  zero dependências de terceiros** (`YAMLParser` em `CaosParser.swift`, ~250 linhas, parser
  recursivo de mapping/sequence/scalar). Usar `kaml` no Android quebraria a paridade "zero
  dependências" que é um valor de design explícito do projeto (ver README: *"No third-party
  dependencies — Parser is stdlib-only"*).

Como o iOS já usa registro explícito (não reflection), o "problema" que o rascunho antigo tentava
resolver com `CaosRegistry` não existe — o Android só precisa do equivalente direto.

---

## Mapeamento de API (Swift → Kotlin)

| Swift (`Sources/Caos/`) | Kotlin (`caos-android`) | Observação |
|---|---|---|
| `CaosError` (enum) | `sealed class CaosError` | `MissingVersion`, `InvalidYaml(line, reason)`, `UnsupportedVersion(version)` |
| `CaosProps` (struct sobre `[String: Any]`) | `data class CaosProps(val data: Map<String, Any?>)` | `string/int/double/bool/nested/array/hexColor` — mesma semântica, incluindo `double()` non-optional com default `0.0` e `hexColor()` validando `#RGB`/`#RRGGBB`/`#AARRGGBB` sem converter pra `Color` |
| `CaosSchema` | `data class CaosSchema(val version: Int, val screens: List<CaosScreen>)` | |
| `CaosContainer` / `CaosEdgeInsets` | data classes equivalentes | `type: String` continua string livre (`vertical`\|`horizontal`\|`grid`), não enum — mantém paridade de schema |
| `CaosShard` | `data class CaosShard(val type: String, val id: String = "", val props: CaosProps = CaosProps())` | |
| `CaosScreen` (classe mutável no Swift) | `data class CaosScreen(val id: String, val containerConfig: CaosContainer, val shardList: List<CaosShard>)` | Kotlin não precisa da mutabilidade que o Swift usa internamente durante o parse — construir imutável direto |
| `YAMLParser` (parser recursivo hand-rolled) | `internal object CaosYamlParser` | port linha-a-linha de `parseMapping`/`parseSequence`/`parseScalar`/`findKeyColon`/`stripInlineComment`/`leadingSpaces`. **Sem dependência externa** |
| `CaosParser.parse(_:)` | `object CaosParser { fun parse(content: String): CaosSchema }` | lança `CaosParseException(val error: CaosError)` em vez de `throws` |
| API v0 deprecated (`CaosParser(content:)`, `getScreens()`, `parseLegacy`) | **não portar** | é compat legado do Swift v0→v1; Android nasce direto em v1 |
| `CaosStore` (`ObservableObject` + Combine) | `class CaosStore` | `shardRegistry: MutableMap<String, @Composable (CaosProps) -> Unit>`; `providers: MutableMap<String, () -> Any?>`; `subjects: MutableMap<String, MutableStateFlow<Any?>>` no lugar de `CurrentValueSubject` |
| `store.register(type:factory:)` (closure) | `store.register(type: String, content: @Composable (CaosProps) -> Unit)` | forma canônica única — ver próxima linha |
| `store.register<V: CaosSwiftUIView>(type:view:)` (genérico + `init(props:)`) | **eliminado, sem substituto direto** | em Compose uma função `@Composable` já é a unidade de composição; não há necessidade de um protocolo tipo `CaosSwiftUIView` nem de reflection pra instanciar. Todo shard se registra como lambda — simplificação real, não perda de funcionalidade |
| `store.view(for:props:)` → `AnyView` | `@Composable fun CaosStore.Render(type: String, props: CaosProps)` | sem type erasure — Compose não precisa de `AnyView` |
| `store.register(key:provider:)` | idêntico, `() -> Any?` | |
| `store.register(key:publisher:)` (Combine `AnyPublisher`) | `store.register(key: String, flow: StateFlow<Any?>)` | |
| `store.resolve<T>(key:)` | `inline fun <reified T> resolve(key: String): T?` | |
| `store.publisher<T>(for:)` → `AnyPublisher<T, Never>` | `fun <T> flowFor(key: String): StateFlow<T?>?` | consumido via `collectAsState()`, não precisa de wrapper `for await` |
| `CaosEnvironment` (`@Environment(\.caosStore)`, `.caosStore(_:)`) | `val LocalCaosStore = staticCompositionLocalOf<CaosStore> { ... }` + `CompositionLocalProvider` | |
| `CaosTapHandler` (`@Environment(\.caosTapAction)`, `.onCaosTap { }`) | `val LocalCaosTapAction = staticCompositionLocalOf<(String, Map<String, Any?>) -> Unit> { { _, _ -> } }` | |
| `CaosSwiftUIView` (protocolo) | **eliminado** | ver linha do `register<V>` acima |
| `CaosScreenView` (`View` + `.task` + `Bundle.path`) | `@Composable fun CaosScreenView(name: String, modifier: Modifier = Modifier)` | carrega via `context.assets.open("$name.yaml")` em `LaunchedEffect(name)`; mesmos 4 estados: loading (`CircularProgressIndicator`) / erro (`CaosError` formatado) / vazio (`"Nenhuma tela encontrada..."`) / renderizado |
| `CaosContainerView` (`ScrollView` + `LazyVStack`/`LazyHStack`/`LazyVGrid`) | `@Composable fun CaosContainerView(screen: CaosScreen)` | `Column`+`verticalScroll` / `Row`+`horizontalScroll` / `LazyVerticalGrid(columns = GridCells.Fixed(2))` conforme `container.type`; padding e spacing idênticos |
| `CaosUnknownShardView` (`#if DEBUG`) | `@Composable fun CaosUnknownShardView(type: String)` | `if (BuildConfig.DEBUG) { Text/Box com aviso } else { }` |
| `ShimmerModifier` / `.shimmer(isActive:)` | `fun Modifier.caosShimmer(isActive: Boolean): Modifier` | `rememberInfiniteTransition` + `Brush.linearGradient`, mesma curva (linear, 1.4s, repeat forever, sem autoreverse) |
| `caos-lint` (executável SPM) | módulo `caos-lint` (plugin `application`, depende de `caos-core`) | mesmas regras (shard sem `type`, `id` duplicado, warning de shard sem `id`) e mesmo formato de saída (`✓`/`⚠`/`✗`) — output deve ser comparável lado a lado com o CLI Swift |

---

## Estrutura do Repositório

```
caos-android/
├── caos-core/            # módulo Kotlin puro (JVM, sem plugin Android/Compose)
│   ├── src/main/kotlin/io/github/andersontizaias/caos/core/
│   │   ├── CaosError.kt
│   │   ├── CaosProps.kt
│   │   ├── CaosSchema.kt
│   │   ├── CaosScreen.kt   (+ CaosContainer, CaosEdgeInsets)
│   │   ├── CaosShard.kt
│   │   ├── CaosParser.kt
│   │   └── CaosYamlParser.kt   (internal)
│   └── src/test/kotlin/.../Fixtures/   ← cópia literal dos 3 fixtures do repo Swift
├── caos-compose/          # módulo Android library, aplica compose
│   ├── src/main/kotlin/io/github/andersontizaias/caos/compose/
│   │   ├── CaosStore.kt
│   │   ├── CaosEnvironment.kt      (LocalCaosStore)
│   │   ├── CaosTapHandler.kt       (LocalCaosTapAction)
│   │   ├── CaosScreenView.kt
│   │   ├── CaosContainerView.kt
│   │   ├── CaosUnknownShardView.kt
│   │   └── CaosShimmer.kt
│   └── src/test/kotlin/...         ← Robolectric + Roborazzi
├── caos-lint/              # módulo JVM CLI (plugin application)
├── caos-sample/            # app Compose de exemplo (BalanceCard + home.yaml, igual ao README Swift)
├── Docs/                   # espelha Docs/ do repo Swift
├── .github/workflows/      # ci.yml, lint.yml, release.yml, nightly.yml
├── build.gradle.kts / settings.gradle.kts
├── README.md / CHANGELOG.md
└── LICENSE (MIT)
```

### Coordenadas de build

- **minSdk:** 24 · **compileSdk / targetSdk:** 36 — alinhado ao `little_bank_android`, primeiro
  consumidor previsto do framework.
- **Compose BOM:** a mesma versão já usada no `little_bank_android`
  (`androidx.compose:compose-bom:2026.05.00`), pra evitar conflito de versão quando o app consumir
  o framework como dependência local/composite build.
- **Kotlin:** JVM target 21 (mesmo do app).

---

## Fases

### Fase 0 — Bootstrap do repositório
`settings.gradle.kts` com os 4 módulos, ktlint + detekt + spotless configurados nos mesmos moldes
do `little_bank_android/app/build.gradle.kts`, CI esqueleto, README stub, `LICENSE` MIT.

### Fase 1 — `caos-core` ✅
Port do parser YAML hand-rolled + modelos de schema. **Critério de aceite central:** copiar os 3
fixtures do repo Swift (`valid_v1.yaml`, `invalid_no_version.yaml`, `edge_cases.yaml`) para
`caos-core/src/test/.../Fixtures/` e escrever testes que comparam a árvore parseada Kotlin com a
árvore parseada Swift (mesmos valores, mesmos tipos) — não só "não crasha".

- [x] `CaosParser.parse()` lê o mesmo YAML v1 do iOS sem modificação no arquivo
- [x] `CaosParseException` carrega `CaosError` com linha/motivo, igual ao Swift
- [x] `CaosProps.nested()` funciona pra `padding` hierárquico
- [x] Módulo compila sem `android.jar` (JVM puro)
- [x] Cobertura ≥ 90% (98.5%, 60 testes — [PR #1](https://github.com/andersontizaias/caos-android/pull/1))

### Fase 2 — `caos-compose` ✅
`CaosStore`, `LocalCaosStore`/`LocalCaosTapAction`, `CaosScreenView`, `CaosContainerView`,
`CaosUnknownShardView`, `Modifier.caosShimmer`.

- [x] `CaosScreenView` renderiza shards registrados corretamente, nos 3 tipos de container
- [x] Shard com `dataKey` atualiza UI automaticamente quando o `StateFlow` emite
- [x] Shard não registrado exibe `CaosUnknownShardView` sem crash, oculto em release
- [x] Testes rodam via **Robolectric** (Roborazzi disponível no classpath pra screenshot tests
      futuros; os testes atuais usam asserções `compose-ui-test`, sem emulador —
      [PR #2](https://github.com/andersontizaias/caos-android/pull/2))
- [x] Cobertura ≥ 90% (93.5%, 26 testes debug / 26 release com 1 skip esperado por `BuildConfig.DEBUG`)

### Fase 3 — `caos-lint` ✅
CLI JVM reusando `caos-core`. Saída textual comparável linha a linha com o `caos-lint` Swift, pra
manter a doc de instalação (`swift run caos-lint` / `./gradlew :caos-lint:run --args=...`)
consistente nos dois READMEs. 94% de cobertura, 12 testes —
[PR #3](https://github.com/andersontizaias/caos-android/pull/3).

### Fase 4 — `caos-sample` ✅
Reproduz o Quick Start do README Swift: `home.yaml` com `BalanceCard`, `MainActivity` registrando o
shard e uma key reativa, exatamente como o exemplo `MyApp` do Swift. Validado rodando de verdade
num emulador (card renderizado + tap disparando `onTap`), não só compilado —
[PR #4](https://github.com/andersontizaias/caos-android/pull/4).

### Fase 5 — CI/CD e Distribuição ✅ (publish real pendente de credenciais)
- [x] `lint.yml`: ktlint + Spotless + detekt em todo push
- [x] `ci.yml`: `./gradlew check` (testes + Robolectric + ktlint + detekt + Spotless + Kover
      ≥90%, já encadeados por task dependency em cada módulo — confirmado com
      `./gradlew :<módulo>:check --dry-run`), mais build do `caos-sample` e um smoke test que
      roda `caos-lint` contra o `home.yaml` do `caos-sample` (dogfooding entre módulos)
- [x] `release.yml`: publica `caos-core` e `caos-compose` no Maven Central via
      `com.vanniktech.maven.publish` (`publishAndReleaseToMavenCentral`), coordenadas
      `io.github.andersontizaias:caos-core` / `io.github.andersontizaias:caos-compose`; também
      gera o fat jar do `caos-lint` (plugin `com.gradleup.shadow`, versão 8.3.x — a linha 9.x
      exige uma API do Gradle mais nova que a 8.14.1 usada aqui) e o APK debug do `caos-sample`,
      anexados a uma GitHub Release
  - **Pendente do lado do usuário, não implementável por mim:** o workflow só publica de
    verdade quando o repo tiver os secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`,
    `GPG_SIGNING_KEY`, `GPG_SIGNING_PASSWORD` configurados — o que exige criar uma conta no
    [Central Portal](https://central.sonatype.com), verificar o namespace
    `io.github.andersontizaias` e gerar uma chave GPG. Nada disso foi criado ou simulado; o
    workflow está pronto pra usar assim que esses secrets existirem.
- [ ] `release-please` pro changelog automático — **decisão consciente de não implementar agora**:
      requer instalar o GitHub App `release-please` no repo, uma ação de conta que cabe ao
      usuário. `version.txt` (fonte única de verdade, lido em `build.gradle.kts` raiz) já está no
      lugar como pré-requisito, caso queira adicionar depois.

### Fase 6 — Docs e certificação de paridade
`README.md` espelhando as seções do Swift (Quick Start, YAML Schema Reference, CaosProps API,
Registering Shards, Data Binding, Tap Events, Loading States, YAML Validation), tabela de paridade
iOS↔Android, `CHANGELOG.md` com entrada `v1.0.0` alinhada à tag do repo Swift.

---

## Tabela de Paridade iOS vs Android (alvo)

| Feature | iOS | Android |
|---|---|---|
| Parser YAML v1 (zero deps) | `CaosParser.swift` (`YAMLParser`) | `CaosParser.kt` (`CaosYamlParser`) |
| Propriedades tipadas | `CaosProps` (struct) | `CaosProps` (data class) |
| Registro de shards | closure explícita (`register(type:factory:)`) | closure explícita (`register(type:content:)`) |
| Container vertical/horizontal/grid | `LazyVStack`/`LazyHStack`/`LazyVGrid` | `Column`/`Row`/`LazyVerticalGrid` |
| Data binding reativo | `CaosStore` + Combine (`CurrentValueSubject`) | `CaosStore` + Coroutines (`MutableStateFlow`) |
| Injeção de contexto | `@Environment(\.caosStore)` | `CompositionLocal<CaosStore>` |
| Tap events | `@Environment(\.caosTapAction)` / `.onCaosTap` | `CompositionLocal` / `LocalCaosTapAction` |
| Shard desconhecido | `CaosUnknownShardView` (`#if DEBUG`) | `CaosUnknownShardView` (`BuildConfig.DEBUG`) |
| Loading shimmer | `ShimmerModifier` / `.shimmer()` | `Modifier.caosShimmer()` |
| CLI de validação | `caos-lint` (SPM executable) | `caos-lint` (Gradle `application`) |
| UI framework | SwiftUI (MV, sem ViewModel) | Jetpack Compose (mesmo padrão MV) |
| Distribuição | Swift Package Manager | Maven Central |
| Schema YAML | v1 compartilhado | v1 compartilhado, byte-a-byte |

---

## Riscos conhecidos

1. **Port do parser YAML** é o item de maior risco/esforço — é um parser recursivo de ~250 linhas
   com regras sutis de indentação, aspas e comentários inline. Mitigação: portar função por função
   na mesma ordem do Swift, e validar com os fixtures compartilhados antes de escrever qualquer
   coisa em `caos-compose`.
2. **`StateFlow` vs `Combine`**: `CurrentValueSubject` aceita `NSNull()` como sentinela de "sem
   valor ainda"; em Kotlin, usar `MutableStateFlow<Any?>(null)` e checar `providers` antes de
   `flows` no `resolve()`, replicando a ordem de prioridade do Swift (`providers` vence `subjects`).
3. **`BuildConfig.DEBUG`** exige o plugin Android aplicado no módulo `caos-compose` (não em
   `caos-core`) — confirmar que `buildFeatures.buildConfig = true` está setado, senão o campo não
   existe.
