# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Android release APK (Firebase distribution)
./gradlew :androidApp:assembleRelease

# Android release AAB (Play Store)
./gradlew :androidApp:bundleRelease

# Run unit tests
./gradlew testDebugUnitTest

# JVM Desktop
./gradlew :composeApp:run

# Clean build
./gradlew clean
```

Release builds require env vars: `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` and the keystore at `magna-keystore.jks` (root).

## Module Structure

Two Gradle modules:

- **`:androidApp`** — thin Android app (`com.android.application`). Contains `MagnaApplication`, `MainActivity`, all `res/`, `google-services.json`. namespace: `com.tick.magna.android`, applicationId: `com.tick.magna`.
- **`:composeApp`** — KMP shared library (`com.android.kotlin.multiplatform.library`). All shared code: features, data, DI, UI. namespace: `com.tick.magna`.

### AGP 9.0 DSL rules
The KMP library uses `kotlin { androidLibrary { ... } }`, **not** `android {}`. `debugImplementation` is unavailable in this module. Dependencies declared in `composeApp` as `implementation` are not visible to `androidApp` consumers — `koin-android` and `napier` must be re-declared explicitly in `androidApp`.

## Architecture

Clean architecture with three layers inside `composeApp/src/commonMain`:

```
data/
  source/remote/api/     — Ktor API interfaces per domain
  source/local/dao/      — SQLDelight DAO wrappers
  repository/            — Repository implementations
  dispatcher/            — CoroutineDispatcher abstraction
  logger/                — Napier logger abstraction
  usecases/
features/
  <feature>/             — ViewModel + State + Screen in the same package
di/
  Modules.kt             — All Koin modules (appModules)
ui/core/                 — Theme, TopBar, Avatar, Shape primitives
```

## DI (Koin)

All modules are registered in `Modules.kt` and exposed as `appModules`. Six modules: `databaseModule`, `dataModule`, `useCaseModule`, `loggingModule`, `viewModelModule`, `platformModule`. Initialized in `MagnaApplication.startKoin { modules(appModules) }`.

## Navigation

Type-safe Compose Navigation using `@Serializable` data classes as route args. The `NavHost` is in `App.kt`. Each destination has an `*Args` class (e.g. `DeputadoDetailsArgs`) that is passed via `savedStateHandle.toRoute<T>()` in the ViewModel.

## Database (SQLDelight)

Database name: `MagnaDatabase`. Schema at `composeApp/src/commonMain/sqldelight/com/tick/magna/`. Migrations at `src/commonMain/sqldelight/migrations/` with verification enabled. Driver factory is platform-specific (`androidMain`, `iosMain`, `jvmMain`).

## Key Conventions

- **One package per feature** — ViewModel, State (sealed interfaces/data classes) and Screen composables in the same directory. When a file grows too large they split into `*ViewModel.kt`, `*State.kt`, `*Screen.kt`.
- **State** is a single `data class` with a nested sealed interface for async content (Loading / Error / Content).
- **No section header comments** — decorative comments like `// ─── Section ───` are not used.
- **Strings** — UI strings use Compose Multiplatform resources (`stringResource(Res.string.*)`), not Android `strings.xml`.
- **Images** — Coil3 with `AsyncImage`; profile pictures loaded from URLs.

## CI/CD

Both workflows require secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `GOOGLE_SERVICES_JSON`.

- `android-release.yml` — builds APK and deploys to Firebase App Distribution. Triggered manually.
- `playstore-upload.yml` — builds AAB and uploads to Play Store internal track. Triggered on `v*` tags or manually.

`google-services.json` is gitignored. Both workflows decode it from `GOOGLE_SERVICES_JSON` (base64 secret) before building.
