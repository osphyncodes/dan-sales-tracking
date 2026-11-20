# Loan Tracker

Loan Tracker is a native Android application for recording clothing loans to customers, collecting repayments, and tracking customer balances. It is designed to work offline with a local Room database.

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

The application ID and namespace are currently `com.example.loantracker`.

## Project layout

```text
app/src/main/java/com/example/loantracker/
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
└── LoanTrackerApp.kt
```

## Data model

The Room database is `app_database` (schema version 3).

| Table | Purpose |
| --- | --- |
| `customers` | Customer name, age, village/cluster IDs, phone, and date added |
| `projects` | Existing loan records: customer, description, date given, optional expected repayment date, and amount given |
| `payments` | Repayments against a loan, including date and payment method |
| `notes` | Notes attached to a loan |

Projects remain the existing database entity and are presented as loans in the app. A blank optional loan amount is stored as zero until it is known. Deleting a customer cascades to their loans; deleting a loan cascades to its payments and notes. Include and test a Room migration for every future schema change.

## Current implementation and gaps

| Capability | Current state |
| --- | --- |
| Customer CRUD | Local create, edit, and delete, with contact editing and device contact import. |
| Customer list | Search and filter all customers, customers with outstanding balances, or customers whose loans are settled. Cards show collected and outstanding totals; long-press for customer details. |
| Fast loan entry | Add or edit a loan from the customer list without navigating away. Loan description and date are required; amount and expected repayment date are optional. |
| Customer loan summary | Tap a customer to see only unpaid loans, then record a repayment directly from that summary. Deeper customer and loan histories remain available. |
| Repayments | Record, edit, and delete repayments; amounts are validated against the remaining loan balance. The full-balance action records the exact amount needed to settle a loan. |
| Loan notes | Add, edit, and delete notes attached to a loan. |
| Dashboard | Date-range and payment-method filters; period income, total loaned and collected, outstanding cash with a progress bar, customers owing, and recent repayments. |
| Currency | Persisted currency symbol/code applied to dashboard and project financial screens. |
| Local backup/restore | Portable passphrase-encrypted database files with byte-based progress, selected-file details, and restore confirmation; the app can be restarted after a successful restore. |
| Google Drive backup | Google Sign-In authorizes direct Drive API access. Encrypted timestamped backups are uploaded to `Drive/LoanTracker/backup`; existing backups in the former `TailorCounterpart` folder remain restorable. The connected Google account's email address is used automatically as the encryption passphrase. |
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
3. Create an Android OAuth client for application ID `com.example.loantracker`, using the SHA-1 fingerprint of the debug signing key for local builds and the release signing key for published builds.
4. Install/update a build signed with the matching key, then use **Settings → Backups → Connect Google Drive**. New backups are uploaded directly to the app's `LoanTracker/backup` Drive folder.

No Web client secret is embedded in the Android app. Manual backups are user-initiated; enabled periodic backups run through WorkManager when network access and Android background scheduling permit.

The manifest declares `READ_CONTACTS`, `INTERNET`, `ACCESS_NETWORK_STATE`, and `USE_BIOMETRIC`. Contact access is used only when the user imports a contact. `FLAG_SECURE` restricts ordinary screenshots and app-switcher previews.

## Remaining work

1. Add on-device tests for schema migration, backup/restore, biometrics, and customer/project UI workflows.
2. Expand automated tests around PIN recovery and database migration.
3. Consider adding backup retention/cleanup controls and on-device tests for Google Drive scheduling and restore behavior.
