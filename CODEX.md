# CODEX.md

Guidance for Codex when working in this repository.

## What this is

**PotLuckHub** (display name **Potluck**) is a native Android app for the Potluck
home-cook / chef marketplace in Singapore. It is a thin Kotlin + Jetpack Compose
client over the live REST backend; there is no local database.

**Audience:** the mobile app is diner-first. Keep the core journey focused on
browse -> book -> eat. Chef onboarding and admin tooling are secondary and should
not lead the mobile experience.

## Source of truth

- **Website:** https://potluckhub.io. Mirror the customer-facing voice: "Home-cooked
  meals, from real Singapore kitchens", neighbourhood language, "makan", trust
  around verified chefs, dietary-friendly meals, and secure SGD payments.
- **Backend/web monorepo:** https://github.com/alfredang/potluck. The Android app
  calls the same production API as iOS.
- **iOS reference app:** https://github.com/alfredang/potluckapp. Keep Android
  feature parity with the diner-facing SwiftUI app where practical.

Cuisine filter slugs must match the backend menu categories exactly:
`malay, chinese, indian, halal, vegetarian, japanese, korean, western`.
Do not add display-only filters that the API cannot serve.

## Architecture

```
app/src/main/java/io/potluckhub/app/
  MainActivity.kt  entry point, tab navigation
  Theme.kt         brand palette and Material theme
  Components.kt    shared UI primitives
  Screens.kt       Explore, Dishes, detail screens
  Account.kt       Bookings, Profile, auth and booking sheets
  Api.kt           REST API wrapper
  Auth.kt          session state and token persistence
  Models.kt        kotlinx.serialization models
```

- Compose + Material 3 only; keep UI additions consistent with the existing warm
  cream/terracotta/teal brand system.
- Explore and Dishes must remain browsable without sign-in.
- Bookings and Profile require authentication.
- Auth tokens persist in SharedPreferences and are synced to `Api.accessToken`.

## Backend API conventions

Base URL: `https://api.potluckhub.io/api/v1`.

- Responses use `{ success, data, error?, pagination? }`; `Api.request` unwraps
  `data` and throws a user-readable `ApiException` for backend errors.
- Prices are integer cents. Always format via `money(...)`.
- Ratings can be strings or numbers depending on endpoint; decode with
  `FlexDoubleSerializer`.
- Register mobile users with role `customer`.
- Account deletion is `DELETE auth/account`; keep this available from Profile
  because the app supports account creation.

## Build and release

```bash
./gradlew :app:assembleDebug
./gradlew :app:bundleRelease
```

Release signing is configured in `app/build.gradle.kts`:

- `POTLUCK_KEYSTORE`, defaulting to `keystore/potluck-release.jks`
- `POTLUCK_KEYSTORE_PASSWORD`
- `POTLUCK_KEY_ALIAS`, defaulting to `potluck`
- `POTLUCK_KEY_PASSWORD`

Google Play package name: `io.potluckhub.app`.

Before publishing:

1. Confirm versionCode/versionName are bumped.
2. Build the signed AAB with `./gradlew :app:bundleRelease`.
3. Verify the customer browse -> dish/chef detail -> booking request flow.
4. Verify demo account login and account deletion against the live API.
5. Upload to Google Play using the configured Play Console workflow or service
   account credentials if present.
