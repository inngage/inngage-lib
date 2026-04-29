# Changelog

All notable changes to the Inngage Android SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

