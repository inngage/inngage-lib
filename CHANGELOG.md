# Changelog

All notable changes to the Inngage Android SDK (Java 4.2.x line) will be documented
in this file. The Kotlin SDK ships separately as the 5.0.0 line (`main` branch).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [v4.2.1] - 2026-08-21

### Fixed

- **Anonymous subscribe no longer sends an empty `identifier`; `registration` is guaranteed non-empty**: When `subscribe(...)` is called without an identifier, the SDK previously fell back to a MAC-based device id that returns empty on Android 6+, so the subscriber was registered with `identifier: ""`. It now uses a stable anonymous id — `Settings.Secure.ANDROID_ID` when available, otherwise a random `UUID` — persisted in preferences and reused, so the device always maps to a single subscriber. Additionally, subscription is now aborted (never sent) if the FCM `registration` token is missing or empty, since a subscriber without a registration cannot receive push.
- **`sendEvent` no longer fails with HTTP 400**: Events were being posted to the deprecated `/v1/events/newEvent/` path, which the backend now rejects with `400 {"message":"Usuário não encontrado ou parâmetros inválidos."}` for every event — meaning event tracking was fully broken across the Java line. Events now post to the current `/v4/event/` endpoint (same request payload, no consumer changes needed). The endpoint is also **environment-aware**: it follows the `env` passed to the most recent `subscribe` (`dev` → `apid.inngage.com.br`, `prod` → `api.inngage.com.br`), defaulting to prod, instead of being hardcoded to production.
- **Release now compiled from source on JitPack**: The `v4.2.0` artifact served by JitPack was a stale, prebuilt binary that did **not** reflect the tagged source — the changelog fixes (conversion fields, request/response logging, geolocation ANR) lived in the source but were missing from the compiled `.aar`, which was byte-identical across every 4.2.0 tag. The library now builds as a standalone, publishable module, so JitPack compiles the real Java source into the released `.aar`. **No public API changes** — drop-in over `v4.2.0`.
- **Geolocation on subscribe no longer hangs**: `subscribe(..., requestGeoLocator = true)` in `InngageService` is now bounded by a global 5s timeout and completes exactly once (cached fix, fresh fix, error, or timeout — whichever comes first). On any problem — permission missing, Play Services error, or no GPS fix within 5s — it tears down the pending location callback/thread and proceeds **without** `lat`/`long` instead of leaving the subscription worker waiting indefinitely. All location work stays off the main thread.

### Changed

- **SDK version string bumped to `4.2.1`**: `InngageConstants.SDK` (reported as the `sdk` field in subscription/event payloads) now reads `4.2.1`, so the backend can distinguish this release from `4.2.0`.
- **Build/publish modernization**: Migrated to Android Gradle Plugin 8.13.2 / Gradle 8.13 (JDK 17), matching JitPack's `openjdk17` environment (the previous AGP 4.2.2 setup could not build there). The Java SDK is now the standalone repository-root module (`namespace br.com.inngage.sdk`), published via `maven-publish` as `com.github.inngage:inngage-lib` with the version tracking the release tag. This 4.2.x line is **Java-only** (no Kotlin/coroutines) — consumers who want the new Kotlin SDK use the `5.0.0` line instead.

---

## [v4.2.0] - 2026-04-29

### Added

- **Geolocation on Subscribe**: The SDK now supports capturing geolocation data during the user subscription/identification process. Location information can be associated with the `subscribe` function when available and authorized by the device.
- **Conversion fields in `sendEvent`**: New conversion fields have been added to the `sendEvent` function, expanding tracking and measurement capabilities for user actions within the app.
- **Request/Response logging**: Logs for API requests and responses have been added, making it easier to analyze, debug, and monitor communications between the SDK and the platform services.

### Fixed

- **In-App Message height adjustment**: Fixed the height of the In-App Message component based on `background_image` and `rich_content` types, ensuring better visual adaptation and more consistent rendering.
- **In-App close button**: Added a close button in the upper-right corner of In-App Messages, allowing users to dismiss the message in a clearer and more intuitive way.
- **Push Notification heads-up display**: Improvements to the Push Messaging service to ensure correct display of heads-up/pop-up notifications and proper URL redirection handling on notification click, making link and deep link behavior more stable and predictable.
- **Geolocation ANR fix**: Fixed an issue related to geolocation that could cause ANR (Application Not Responding) in certain scenarios, improving app stability during location data collection.

### Changed

- **Gradle & build configuration**: Adjustments to Gradle settings, project properties, required permissions, and `jitpack.yml` to improve the build, distribution, and integration process of the SDK in Android projects.

---

## [v4.1.0] - Previous Release

- See git history for details.
