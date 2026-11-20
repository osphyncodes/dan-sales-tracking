# Tailor's Counterpart

Tailor's Counterpart is a native Android application for managing tailoring customers, garment projects, measurements, payments, and notes. It is designed to work offline with a local Room database.

> **Project status:** Core offline workflows and optional scheduled Google Drive backups are implemented. Google Drive backups require Google Cloud OAuth configuration and a connected account; see [Current implementation and gaps](#current-implementation-and-gaps).

## Technology

| Area | Implementation |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| Dependency injection | Hilt |
| Local database | Room / SQLite |
| Preferences | DataStore Preferences |
| Backup encryption | Passphrase-derived AES-256-GCM |
| Minimum Android version | Android 7.0 (API 24) |
| Compile / target SDK | 34 |
| Java / Kotlin target | Java 17 |

The application ID and namespace are currently `com.example.tailorscounterpart`.

## Project layout

```text
app/src/main/java/com/example/tailorscounterpart/
├── data/local/
│   ├── daos/           Room queries and mutations
│   ├── database/      Room database, migrations, and Hilt providers
│   ├── entities/      Room entities and relationship models
│   ├── repository/    Data-access wrappers
│   ├── EncryptedBackupManager.kt
│   ├── PinStore.kt
│   └── UserPreferences.kt
├── domain/finance/    Dashboard calculations
├── functions/         Contact lookup and shared formatting helpers
├── navigation/        Main and project destinations
├── ui/
│   ├── components/    Reusable Compose UI
│   ├── screens/       Dashboard, customer, project, settings, and lock screens
│   └── theme/         Compose theme
├── viewmodel/         Screen and business state
├── MainActivity.kt    Compose host, app lock, and navigation chrome
└── TailorsCounterpartApp.kt
```

## Data model

The Room database is `app_database` (schema version 3).

| Table | Purpose |
| --- | --- |
| `customers` | Customer name, age, gender, and creation date |
| `contacts` | Customer phone, email, address, or other contact values |
| `projects` | Customer garment/job, due date, status, and quoted price |
| `measurements` | Reusable measurement type names |
| `project_measurements` | Measurement values captured for a project |
| `payments` | Project payments, dates, and payment methods |
| `notes` | Notes attached to tailoring projects |

Deleting a customer cascades to its projects and contacts. Deleting a project cascades to its payments, notes, and project measurements. The version 2-to-3 migration corrects the note-to-project foreign key and keeps notes linked to existing projects. Include and test a Room migration for every future schema change.

## Current implementation and gaps

| Capability | Current state |
| --- | --- |
| Customer CRUD | Local create, edit, and delete, with contact editing and device contact import. |
| Customer filters | Search, finished/unfinished project status, outstanding balance, and customer creation date range. |
| Project CRUD | Create, edit, delete, detail, and `In Progress` / `Cancelled` / `Completed` status selection per customer. |
| Project detail actions | More-menu actions for edit, delete, status update, quick payment, and quick measurement entry. |
| Project payments | Room-backed add, edit, delete, method tracking, and totals; payment amounts are limited to the remaining project balance. |
| Project measurements | Room-backed add, edit, and removal; measurement types can be created during entry. |
| Project notes | Room-backed add, edit, and delete. |
| Dashboard | Date-range and payment-method filters; period income, cumulative income, outstanding balances, customer and cumulative project counts, projects in progress, and recent payments. |
| Currency | Persisted currency symbol/code applied to dashboard and project financial screens. |
| Local backup/restore | Portable passphrase-encrypted database files with byte-based progress, selected-file details, and restore confirmation; the app can be restarted after a successful restore. |
| Google Drive backup | Google Sign-In authorizes direct Drive API access. Encrypted timestamped backups are uploaded to `Drive/TailorCounterpart/backup`; restore offers only the newest backup. The connected Google account's email address is used automatically as the encryption passphrase. |
| Automatic cloud backup | Optional daily, weekly, or 30-day periodic uploads through WorkManager, requiring a connected Google account and network access. Android may defer background work. |
| App lock | PIN creation, verification, and change, recovery-question setup, and optional device biometric unlock. |
| Tests | Unit tests cover financial summary calculations and project navigation. Database migration, backup, biometric, and UI flows still need broader coverage. |

## Build and test

Open the repository in Android Studio and allow Gradle sync to finish. On Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The debug APK is written to `app\build\outputs\apk\debug\app-debug.apk`.

## Backup and privacy

Backups use a passphrase-derived AES-GCM key. For Google Drive backups, the connected account's email is supplied automatically on both backup and restore, so the user does not enter a separate passphrase. The email is not a strong secret; anyone with access to the Drive account and backup may be able to recover the data. Restoring also requires the same email address used when the backup was created. The app validates the restored SQLite database before replacing local data. Restoring replaces the current database, so create a backup first. Google Drive access uses Google Sign-In with the restricted `drive.file` scope and the Drive REST API. The account handoff from Google Sign-In is exempt from the app's background PIN lock for up to five minutes, including when Android recreates the activity.

### Google Drive API setup

1. In Google Cloud Console, create or select a project and enable the Google Drive API.
2. Configure the OAuth consent screen and add tester accounts if the app remains in testing.
3. Create an Android OAuth client for application ID `com.example.tailorscounterpart`, using the SHA-1 fingerprint of the debug signing key for local builds and the release signing key for published builds.
4. Install/update a build signed with the matching key, then use **Settings → Backups → Connect Google Drive**. Backups are uploaded directly to the app's `TailorCounterpart/backup` Drive folder.

No Web client secret is embedded in the Android app. Manual backups are user-initiated; enabled periodic backups run through WorkManager when network access and Android background scheduling permit.

The manifest declares `READ_CONTACTS`, `INTERNET`, `ACCESS_NETWORK_STATE`, and `USE_BIOMETRIC`. Contact access is used only when the user imports a contact. `FLAG_SECURE` restricts ordinary screenshots and app-switcher previews.

## Remaining work

1. Add on-device tests for schema migration, backup/restore, biometrics, and customer/project UI workflows.
2. Expand automated tests around PIN recovery and database migration.
3. Consider adding backup retention/cleanup controls and on-device tests for Google Drive scheduling and restore behavior.
