# Architecture

How Magna is put together: modules, the data pipeline from the Câmara API to the screen,
navigation, ViewModels, dependency injection, observability and the build/release pipeline.
Per-feature detail is in [features/](features/README.md), everything about the Câmara API is in [api/](api/README.md), and the domain objects and mapping rules are in [domain.md](domain.md).

Paths are relative to `composeApp/src/commonMain/kotlin/com/tick/magna/` unless they start with
a top-level directory.

## Modules and targets

| Module | Plugin | What lives there |
|---|---|---|
| `:androidApp` | `com.android.application` | `MagnaApplication`, `MainActivity`, `res/`, `google-services.json`, Firebase analytics and Crashlytics, R8 rules. applicationId `com.tick.magna`. |
| `:composeApp` | `com.android.kotlin.multiplatform.library` | All shared code: features, data, DI, UI, SQLDelight schema. |

`composeApp` targets Android (`kotlin { androidLibrary { } }`, not `android { }`), `iosArm64`,
`iosSimulatorArm64` and `jvm` (desktop). Android is the only target that ships. iOS and desktop
compile, but some features are absent on them: the santinho has no keystore there, and there is
no database fallback on either.

AGP 9 caveats: `debugImplementation` does not exist in the KMP module, and a dependency declared
`implementation` in `composeApp` is not visible to `androidApp`. That is why `koin-android` and
`napier` are declared again in `androidApp`.

`MainActivity` is a `FragmentActivity` only because `BiometricPrompt` needs one.

## Data pipeline

```
Câmara API ──Ktor──▶ DTO ──mapper──▶ SQLDelight table ──Flow──▶ Repository ──▶ ViewModel ──StateFlow──▶ Screen
                                           ▲                        │
                                           └──── refresh writes ────┘
```

- **Network.** `data/source/remote/HttpClientFactory.kt` builds a single `HttpClient`:
  - `StripAcceptCharset` removes the `Accept-Charset` header in the Send phase. The Câmara
    gateway answers **403** to any request that carries it, and Ktor adds it by default.
  - It has timeouts, retries only on server errors (never on timeouts), sets
    `expectSuccess = true`, and reports every API failure to analytics from this one place.
  - APIs: `data/source/remote/api/*Api.kt`, one per domain, each behind an interface. DTOs are in
    `dto/` and `response/`.
- **Database.** SQLDelight, `MagnaDatabase`, file `magna.db`.
  - Schema: `composeApp/src/commonMain/sqldelight/com/tick/magna/*.sq`. Migrations:
    `sqldelight/migrations/N.sqm`, with `1.db`/`2.db` baselines and verification on.
  - DAOs in `data/source/local/dao/` wrap the generated queries behind interfaces and expose
    Flows on `DispatcherInterface.io`.
  - Verification checks the schema, not the data. On Android, a failed migration deletes the
    database and starts over (`composeApp/src/androidMain/.../DatabaseDriverFactory.android.kt`).
    See `CLAUDE.md`.
- **Repositories** (`data/repository/`) own every decision about when to hit the network.
  - Everything is scoped by `User.legislaturaId` through `userDao.getUser().flatMapLatest { }`,
    so a term switch re-reads everything by itself.
  - `Resource<T>` (`Loading / Error / Content(data, isRefreshing)`) is the async shape.
  - `cachedRecord` and `cachedList` in `data/repository/Resource.kt` emit the cache, refresh
    inside the flow (so leaving the screen cancels the request), and keep following the cache.
  - Freshness goes through `isCacheFresh` in `CachePolicy.kt`: a finished term is fresh forever,
    a timestamp in the future counts as stale, and "never fetched" is not the same as "empty".
  - One-shot reads that a screen owns return `Result<T>` instead of a flow.
- **Use cases** (`data/usecases/`). Only one: `SyncUserInformationUseCase`, the first-run and
  term-switch sync. See [features/home-e-legislatura.md](features/home-e-legislatura.md).
- **Rules that apply to every layer.**
  - Always rethrow `CancellationException`.
  - A single-row read that feeds the UI uses `mapToOneOrNull` and must still emit.
  - A term's data is written whole or not at all.

## Navigation

Everything is in `App.kt`.

- **Type-safe Compose Navigation.** Each destination is a `@Serializable` `*Args` object or data
  class next to its screen (`HomeArgs`, `DeputadoDetailsArgs(deputadoId)`, and so on). The start
  destination is `HomeArgs`. There are 11 destinations: Home, Santinho, DeputadosSearch,
  DeputadoDetails, ComissoesList, ComissaoPermanenteDetail, PartidosList, PartidoDetails,
  VotacaoDetail, ProposicoesList, ProposicaoDetails.
- **Screens receive the `NavController`** and navigate with `navController.navigate(SomeArgs(id))`.
  Home sections get a `navigateTo: (Any) -> Unit` lambda. Adding a destination takes three
  things: the `*Args` class, a `composable<*Args> { Animado { Screen(...) } }` entry, and the
  ViewModel registration (see DI).
- **Arguments reach the ViewModel through `SavedStateHandle`**:
  `savedStateHandle.toRoute<XArgs>()`. The screen never passes ids to its ViewModel by hand.
  The one exception is `ComissaoPermanenteDetailArgs` in `App.kt`, which also passes
  `parametersOf(id)` to `koinViewModel`, although its Koin definition destructures a
  `SavedStateHandle` and the ViewModel reads the route from that handle. It has shipped like
  this since January and the screen works, but it is the odd one out. Don't copy it, and if you
  remove it, open a committee on a device to confirm.
- **Transitions.** A single `SharedTransitionLayout` wraps the `NavHost`, and the enter/exit
  animations are in `ui/core/navigation/Transicoes.kt` (300 ms).
  - Each destination body is wrapped in `Animado { }`, which provides
    `LocalAnimatedVisibilityScope`. `LocalSharedTransitionScope` is provided above the `NavHost`.
    No screen signature mentions animation.
  - Shared elements live in `ui/core/navigation/ElementoCompartilhado.kt`.
    `Modifier.elementoCompartilhado(chave)` handles shapes and photos, and the text variant uses
    `sharedBounds`. Keys come from `ChaveCompartilhada` and name the **thing, not the place**
    (`deputado-foto-<id>`), so any list that shows the same person flies into their screen.
  - Both scopes are null in previews, and the modifiers become no-ops there.
- **Screen chrome.** Every screen uses `ui/component/MagnaScreen.kt` (title, back arrow, `area`,
  actions, a collapsing `enterAlways` bar). The `area` (`ui/core/theme/MagnaArea.kt`) colours
  the bar.
- **Screen tracking** is in one place: a `LaunchedEffect` on `currentBackStackEntryFlow` in
  `App.kt` reports `ScreenView(route.toScreenName())`. Routes carry placeholders, not real ids.
  Firebase's automatic screen reporting is off in the manifest. `ScreenName.relatavel()`
  excludes `Santinho`.

## ViewModels

- **One per screen**, plus one per Home section component (`RecentDeputadosViewModel`,
  `RecentProposicoesViewModel`, `ComissoesPermanentesViewModel`, `PartidosComponentViewModel`,
  and the Home's `SantinhoViewModel`).
  - Screens get them with `viewModel: XViewModel = koinViewModel()` as a default parameter.
  - They are scoped to the navigation back-stack entry, so a component's ViewModel lives as long
    as the Home destination does. The santinho relies on that for its one-time pulse.
- **Shape.** `private val _state = MutableStateFlow(XState())` plus
  `val state = _state.asStateFlow()`. Work runs in `viewModelScope.launch(dispatcher.io)`. User
  input arrives as `processAction(XAction)`, a sealed interface, or as named `onX()` functions.
- **State.** A single `data class` per screen, with nested sealed interfaces for each async part
  (`Loading / Empty / Error / Content`). `Content` is never built with an empty list. The mapping
  from `Result`/`Resource` to state lives in top-level `internal fun xStateFor(...)` functions,
  so it can be tested without a ViewModel.
- **Concurrency rules** (all of them come from real bugs):
  - Always use `_state.update { }`. Several coroutines write on IO, and read-then-assign loses
    updates.
  - A `combine` copies only the fields it owns into the current state. Building a whole state
    there once reset the selected tab on every emission.
  - Slow independent work (the vote sweep, committee seats) runs in its own coroutine, outside
    the `combine`.
  - Expensive tabs load on demand (committee presidents).
- **Dependencies** are always interfaces: repositories, `DispatcherInterface`,
  `AppLoggerInterface`, `AnalyticsInterface`. Tests use hand-written fakes, not a mocking library.

## Dependency injection

Koin. Everything is registered in `di/Modules.kt` as `appModules`:

| Module | Contents |
|---|---|
| `platformModule` (expect/actual) | `DatabaseDriverFactory`, `CofreLocalInterface`, `AppBuildConfig` |
| `databaseModule` | `SqlDriver`, `MagnaDatabase`, every `*Queries`, every DAO (`single<XDaoInterface>`) |
| `dataModule` | `DispatcherInterface`, `HttpClient`, every API, every repository, `SantinhoRepository`, `LeitorDeCorDoLogo` |
| `useCaseModule` | `factoryOf(::SyncUserInformationUseCase)` |
| `loggingModule` | `AppLoggerInterface` (Napier), `AnalyticsInterface` (`LogAnalytics` by default) |
| `viewModelModule` | `viewModel { ... }`. Routes with arguments use `viewModel { (handle: SavedStateHandle) -> XViewModel(handle, get(), ...) }` |

- Start-up: `MagnaApplication.startKoin { androidContext(...); modules(appModules + androidAnalyticsModule) }`.
  `androidAnalyticsModule` comes **last** so its `FirebaseAnalyticsTracker` overrides `LogAnalytics`
  (`di/AnalyticsOverrideTest.kt` guards this).
- Bindings use positional `get()`. Adding a constructor parameter means updating the `get()`
  count in `Modules.kt`. The compiler catches a wrong count, but not two parameters of the same
  interface swapped.
- Singletons for everything except ViewModels and the use case.
- Koin resolves lazily, so a platform binding can depend on something from `loggingModule`
  whatever order the modules are listed in (the Android `DatabaseDriverFactory` takes the logger).

## Observability

- **Logging.** Napier, behind `AppLoggerInterface`. Debug builds log to Logcat
  (`DebugAntilog`). Release builds use `CrashlyticsAntilog`
  (`androidApp/.../logging/CrashlyticsAntilog.kt`): INFO and above become Crashlytics breadcrumbs,
  and `logger.e(msg, throwable)` is recorded as a **non-fatal**. Deciding whether a build is
  debug is done at runtime with `FLAG_DEBUGGABLE`, not with `BuildConfig`.
- **Analytics.** `AnalyticsInterface` with typed events in `data/analytics/AnalyticsEvent.kt`.
  Firebase on Android, a logger elsewhere. The santinho reports nothing, on purpose.

## Build and release pipeline

| | Trigger | Runs tests | Produces | Goes to |
|---|---|---|---|---|
| `.github/workflows/android-release.yml` | manual only | yes: `verifySqlDelightMigration`, `:composeApp:jvmTest`, `:androidApp:testDebugUnitTest` | signed APK | Firebase App Distribution, "Testers" group |
| `.github/workflows/playstore-upload.yml` | **any tag push** (`'*'`), or manual | yes: the same `test` job, and `deploy` needs it | signed AAB | Play Store **production**, no staged rollout, notes from `distribution/whatsnew/` |

- **Pushing a tag publishes to everyone.** The tests gate the upload, but they are unit tests
  only: nothing opens the app. Try the `minified` build on a device before tagging. Nothing runs
  on a push to `main` or on a PR, so a broken `main` is only discovered at release time.
- **Versioning** is `versionCode` / `versionName` in `androidApp/build.gradle.kts` (8 / "2.0.4"
  at the time of writing). Bump both and update `distribution/whatsnew/whatsnew-pt-BR` (Play
  Store limit: 500 characters) before tagging.
- **Signing.** `magna-keystore.jks` at the repo root, plus env vars `KEYSTORE_PASSWORD`,
  `KEY_ALIAS` and `KEY_PASSWORD`. CI decodes the keystore and `google-services.json` from base64
  secrets.
  - Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`,
    `GOOGLE_SERVICES_JSON`, `FIREBASE_APP_ID`, `FIREBASE_SERVICE_ACCOUNT`,
    `PLAY_STORE_SERVICE_ACCOUNT`.
- **Build types.**
  - `release`: R8 (`isMinifyEnabled`, `isShrinkResources`, `androidApp/proguard-rules.pro`),
    uploads the Crashlytics mapping file.
  - `minified`: same as release, but signed with the debug key and not debuggable. Use it to find
    what R8 breaks (stripped serializers, classes only reached by reflection) without the
    release keystore. It does not upload a mapping file.
- **Local commands** are in `CLAUDE.md` (`assembleRelease`, `bundleRelease`,
  `testDebugUnitTest`, `:composeApp:run`). On this machine, keep Gradle runs for the end of a
  block of work: each one takes 30 s to 3 min.
