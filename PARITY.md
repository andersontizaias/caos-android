# iOS vs Android Parity

Feature-by-feature comparison between [Caos](https://github.com/andersontizaias/Caos) (SwiftUI)
and this repository (Jetpack Compose). Full API mapping, with the reasoning behind every
deliberate difference, is in [`PLAN_ANDROID.md`](./PLAN_ANDROID.md).

| Feature | iOS | Android |
|---|---|---|
| YAML v1 parser (zero deps) | `CaosParser.swift` (`YAMLParser`) | `CaosParser.kt` (`CaosYamlParser`) |
| Typed properties | `CaosProps` (struct) | `CaosProps` (data class) |
| Shard registration | explicit closure (`register(type:factory:)`) | explicit closure (`register(type:content:)`) |
| Vertical/horizontal/grid container | `LazyVStack`/`LazyHStack`/`LazyVGrid` (+ `ScrollView`) | `LazyColumn`/`LazyRow`/`LazyVerticalGrid` (self-scrolling) |
| Reactive data binding | `CaosStore` + Combine (`CurrentValueSubject`) | `CaosStore` + Coroutines (`MutableStateFlow`) |
| Context injection | `@Environment(\.caosStore)` | `CompositionLocal` (`LocalCaosStore`) |
| Tap events | `@Environment(\.caosTapAction)` / `.onCaosTap` | `CompositionLocal` (`LocalCaosTapAction`) |
| Unknown shard | `CaosUnknownShardView` (`#if DEBUG`) | `CaosUnknownShardView` (`BuildConfig.DEBUG`) |
| Loading shimmer | `ShimmerModifier` / `.shimmer()` | `Modifier.caosShimmer()` |
| Validation CLI | `caos-lint` (SPM executable) | `caos-lint` (Gradle `application` + fat jar) |
| UI framework | SwiftUI (MV, no ViewModel) | Jetpack Compose (same MV pattern) |
| Distribution | Swift Package Manager | Maven Central (config ready, publish pending secrets) |
| YAML schema | v1, shared | v1, shared, byte-for-byte |
