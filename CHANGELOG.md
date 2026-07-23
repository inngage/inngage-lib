# Changelog

All notable changes to the Inngage Android SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] - 5.0.0

### Changed

- **BREAKING — `sendEvent` contract**: `InngageClient.sendEvent` now takes `context` as its first parameter and `identifier` became optional: when provided it is sent as `identifier`; otherwise the SDK sends the persisted FCM token as `registration` (fails with a log if neither is available — `subscribe()` must run first). New optional conversion parameters: `conversionValue` (number or string), `conversionNotid` and `conversionEvent` (default `false`); `conversion_value`/`conversion_notid` are only sent when `conversionEvent` is `true`, and `conversion_event` is always sent with its value. `event_values` is now omitted from the request when not provided (previously sent as an empty string).
- **Push notification accent color**: `PushConfig.notificationColor` is now applied to the notifications built by the SDK (it was accepted and ignored). Since Inngage pushes are FCM data messages, the SDK renders every notification itself — icon and color from `PushConfig` apply in both foreground and background. `PushConfig.COLOR_UNSET` (default) keeps the system color.

- **In-App Message v2 endpoint**: The In-App fetch now targets the production API (`https://api.inngage.com.br/v4/message/objectMessage`) with the new request contract `{ appId, registration, firstAccess, channelId }` (`channelId` fixed at `6`). The `deviceUuid` and `mobileType` fields were removed from the request. Development-only hardcoded values (local emulator base URL and fallback `appId`/device UUID) were removed; the fetch is now skipped until a subscription has persisted `appId` and the FCM token.
- **In-App Message v2 render model**: The validated `InAppMessageV2` is now handed to `InAppMessageV2Activity` as a `Serializable` Intent extra (`EXTRA_MESSAGE`) instead of being re-serialised to a JSON string and re-parsed on the other side. This removes a duplicated (and previously divergent) JSON parser from the Activity. Banner vs. carousel is decided purely by slide count (1 = banner, >1 = carousel with a dot indicator).
- **`firstAccess` flag**: Now flips to `false` only after a successful (HTTP 200) In-App fetch, so a failed first attempt still counts as first access on retry.
- **FCM token persistence**: The FCM registration token is now persisted on every `subscribe()` call and on FCM token refresh (`onNewToken`), keeping it available and up to date for other APIs.

### Removed

- **BREAKING — `environment` parameter**: Removed from `InngageClient.handleNotification` (and the internal `InngagePush` facade). All API calls now target the production environment; the `dev` endpoint and the `InngageEnvironment` enum were removed.
- **Legacy push-triggered In-App flow**: Removed the legacy In-App path that rendered a fixed two-button layout from push `Intent` extras (`InAppActivity`, `InAppUseCase`, `InAppKeys`, and the `additional_data` branch in `NotificationRouter`). In-App Messages are now exclusively fetched from the backend (v2).
- **Automatic In-App triggers**: The SDK no longer fetches/displays In-App Messages on its own. Removed the app-foreground trigger (`InAppForegroundTrigger`, registered during `subscribe()`) and the post-subscription fetch in `SubscriptionWorker`. In-App display is now exclusively app-driven: the host app decides when to call `InngageClient.showInAppMessage(context)` (e.g. on each screen load).

### Added

- **In-App Message action callbacks**: `InngageClient.showInAppMessage` now accepts an optional `handledBySdk` flag (default `true`) plus `onActions` and `onActionClick` callbacks. With `handledBySdk = true` the SDK resolves button/background actions itself as before. With `handledBySdk = false` the SDK still renders the message but does **not** navigate; instead it hands the actions to the host app: `onActions` fires once, on display, with the full list of `InAppActionData`, and `onActionClick` fires on each button/background tap with that specific action — including its `url` — so the app can, e.g., open the screen matching the tapped button's URL. Each `InAppActionData` carries `slideIndex`, `buttonIndex` (`-1` for background), `source`, `buttonText`, `type`, `url` and `metadata`. Tapping still dismisses the message. The existing `showInAppMessage(context)` signature is unchanged (non-breaking).
- **`addUserData` method**: New `InngageClient.addUserData(context, appToken, identifier?, customFields?, email?, phoneNumber?)` posts a `fieldsRequest` payload to `/v4/subscription/addCustomField` to enrich an existing subscriber profile (e.g. after login, with `subscribe()` called anonymously on the login screen). Only `app_token` is mandatory; null fields are omitted. When `identifier` is not provided, the identifier persisted by the last `subscribe()` is used so the data links to the subscriber created earlier.
- **Anonymous subscription identity**: When `subscribe()` is called without `identifier`, the SDK now generates a random per-installation UUID (persisted and reused across launches) and sends it as both `identifier` and `uuid` — the `identifier` field is never empty anymore. When `identifier` is provided, `uuid` still carries the installation UUID. The legacy device-id resolution (Wi-Fi MAC → IMEI → ANDROID_ID), unreliable on modern Android, was replaced by this UUID.
- **Geolocation on Subscribe**: The SDK now supports capturing geolocation data during the user subscription/identification process. Location information can be associated with the `subscribe` function when available and authorized by the device.
- **Conversion fields in `sendEvent`**: New conversion fields have been added to the `sendEvent` function, expanding tracking and measurement capabilities for user actions within the app.
- **Request/Response logging**: Logs for API requests and responses have been added, making it easier to analyze, debug, and monitor communications between the SDK and the platform services.

### Fixed

- **HTTP 2xx treated as error**: `HttpClient` only accepted HTTP 200 as success; any other 2xx (e.g. `201 Created` returned by `/v4/subscription/`) was logged as an error and failed the flow even though the operation succeeded. All 2xx statuses are now treated as success.
- **In-App Message v2 not rendering**: The parser now accepts the flat production payload — `type`/`style`/`media` at the root with slides directly under `media.items`. Previously the code required an `enabled` flag and a `media.carousel` wrapper (neither present in production), so nothing was shown. The nested/wrapped formats and an explicit `enabled:false` (suppresses the message) are still honoured.
- **In-App Message v2 broken/missing images**: A slide whose image is absent, or whose URL is unreachable/broken, now collapses the image area instead of leaving an empty/broken placeholder box.
- **In-App Message height adjustment**: Fixed the height of the In-App Message component based on `background_image` and `rich_content` types, ensuring better visual adaptation and more consistent rendering.
- **In-App close button**: Added a close button in the upper-right corner of In-App Messages, allowing users to dismiss the message in a clearer and more intuitive way.
- **Push Notification heads-up display**: Improvements to the Push Messaging service to ensure correct display of heads-up/pop-up notifications and proper URL redirection handling on notification click, making link and deep link behavior more stable and predictable.
- **Geolocation ANR fix**: Fixed an issue related to geolocation that could cause ANR (Application Not Responding) in certain scenarios, improving app stability during location data collection.

### Changed

- **Gradle & build configuration**: Adjustments to Gradle settings, project properties, required permissions, and `jitpack.yml` to improve the build, distribution, and integration process of the SDK in Android projects.

---

## [v4.1.0] - Previous Release

- See git history for details.

