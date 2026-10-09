# Finance Tracker — Technical Documentation & Architecture

This document covers technical specifications, architecture decisions, build steps, test runner usage, and security considerations for developers and contributors.

---

## Table of Contents

1. [Architecture & Codebase Layout](#architecture--codebase-layout)
2. [Prerequisites & Toolchain](#prerequisites--toolchain)
3. [Building From Source](#building-from-source)
4. [Testing & Quality Verification](#testing--quality-verification)
5. [Data Integrity & Security Safeguards](#data-integrity--security-safeguards)
6. [Backup & Schema Format](#backup--schema-format)
7. [License](#license)

---

## Architecture & Codebase Layout

Finance Tracker follows a single-activity architecture built with modern Android Jetpack libraries. Presentation, business logic, and local storage layers are strictly separated:

- **Presentation**: Jetpack Compose (Material 3) with unidirectional data flow (UDF). Screens observe immutable UI state flows from ViewModels.
- **Local Persistence**: Room SQLite database with Flow-based DAOs and transactional operations.
- **Navigation**: Navigation Compose with a 4-tab bottom navigation bar (`Dashboard`, `Add`, `History`, `Categories`) and pushed sub-routes for reminders, export, and backup.
- **Charts**: Vico library for cashflow bar charts and spending trend lines.
- **Serialization**: `kotlinx.serialization` for backup file parsing and generation.
- **Reminders**: Android `AlarmManager` with `BroadcastReceiver` to post notifications on scheduled due dates across device reboots.

```
Finance-Track/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/financetracker/app/
│   │   │   │   ├── MainActivity.kt               # Single activity host
│   │   │   │   ├── backup/                       # JSON backup codec, validator, and restorer
│   │   │   │   │   ├── BackupCodec.kt
│   │   │   │   │   ├── BackupModels.kt
│   │   │   │   │   ├── BackupRestorer.kt
│   │   │   │   │   └── BackupValidator.kt
│   │   │   │   ├── data/                         # Room database, DAOs, and entities
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── BackupDao.kt
│   │   │   │   │   │   ├── CategoryDao.kt
│   │   │   │   │   │   ├── RecurringItemDao.kt
│   │   │   │   │   │   └── TransactionDao.kt
│   │   │   │   │   └── entity/
│   │   │   │   │       ├── Category.kt
│   │   │   │   │       ├── RecurringFrequency.kt
│   │   │   │   │       ├── RecurringItem.kt
│   │   │   │   │       └── Transaction.kt
│   │   │   │   ├── export/                       # CSV and JSON export formatting
│   │   │   │   │   └── Exporter.kt
│   │   │   │   ├── reminder/                     # AlarmManager scheduler and notification receiver
│   │   │   │   │   ├── ReminderReceiver.kt
│   │   │   │   │   └── ReminderScheduler.kt
│   │   │   │   └── ui/                           # Jetpack Compose UI and ViewModels
│   │   │   │       ├── backup/                   # Backup and restore screen
│   │   │   │       ├── categories/               # Category management screen
│   │   │   │       ├── components/               # Shared UI components, formatters, and icons
│   │   │   │       ├── dashboard/                # Main dashboard screen
│   │   │   │       ├── entry/                    # Add/edit transaction screen
│   │   │   │       ├── export/                   # Export screen
│   │   │   │       ├── history/                  # Transaction history list screen
│   │   │   │       ├── recurring/                # Recurring bill reminders screen
│   │   │   │       └── theme/                    # Material 3 theme and color tokens
│   │   │   └── res/                              # Drawables, mipmaps, and app resources
│   │   ├── test/                                 # Unit tests (134 tests across 22 test suites)
│   │   └── androidTest/                          # Compose UI and Room instrumentation tests
│   └── build.gradle.kts                          # Module build script and dependency declarations
├── gradle/
│   ├── libs.versions.toml                        # Version catalog (dependencies and plugins)
│   └── wrapper/                                  # Gradle wrapper 9.3.1
└── settings.gradle.kts
```

---

## Prerequisites & Toolchain

- **JDK**: Java Development Kit 21 or higher (or JDK 17 with Gradle toolchain auto-provisioning; Android Studio's bundled JBR 17/21 works out of the box).
- **Android SDK**:
  - `minSdk`: 26 (Android 8.0 Oreo)
  - `targetSdk`: 36 (Android 16)
  - `compileSdk`: 36
- **Gradle**: Managed automatically via the included Gradle wrapper (`gradlew` / `gradlew.bat`), using Gradle 9.3.1.

---

## Building From Source

### Using Android Studio

1. Open Android Studio (Ladybug or newer recommended).
2. Select **Open** and choose the `Finance-Track` folder.
3. Allow Gradle to complete the project sync.
4. Select the `app` configuration in the toolbar and click **Run** (or `Shift + F10`).

### Using the Command Line

To assemble the debug APK:

```bash
# macOS / Linux
./gradlew :app:assembleDebug

# Windows (PowerShell)
.\gradlew.bat :app:assembleDebug
```

The resulting APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

To install directly onto an active device or running emulator:

```bash
# macOS / Linux
./gradlew :app:installDebug

# Windows (PowerShell)
.\gradlew.bat :app:installDebug
```

---

## Testing & Quality Verification

### Unit Test Suite

The project includes 134 local unit tests covering ViewModels, Room DAOs, backup encoding and validation, reminder calculation, and currency formatters:

```bash
# macOS / Linux
./gradlew :app:testDebugUnitTest

# Windows (PowerShell)
.\gradlew.bat :app:testDebugUnitTest
```

### Static Analysis (Android Lint)

Android Lint checks are configured to catch potential API and resource issues. The codebase maintains a clean 0-error status:

```bash
# macOS / Linux
./gradlew :app:lintDebug

# Windows (PowerShell)
.\gradlew.bat :app:lintDebug
```

### Instrumented Compose UI Tests

UI interactions, state restoration across configuration changes, and end-to-end user flows are tested via instrumented tests on an emulator or physical device (API 26+):

```bash
# macOS / Linux
./gradlew :app:connectedDebugAndroidTest

# Windows (PowerShell)
.\gradlew.bat :app:connectedDebugAndroidTest
```

---

## Data Integrity & Security Safeguards

- **Zero Network Permissions**: The app does not declare `android.permission.INTERNET`. No network sockets can be opened, ensuring complete offline data privacy.
- **Backup Payload Validation**: `BackupValidator` validates all JSON backup payloads before SQLite insertion, enforcing positive amounts (`amount > 0`), valid category associations, non-empty names, and string length boundaries (`MAX_NOTE_LENGTH = 100`, `MAX_CATEGORY_NAME_LENGTH = 36`).
- **CSV Formula Injection Defense**: In `Exporter.kt`, fields starting with formula characters (`=`, `+`, `-`, `@`) or tab characters are prefixed with a single quote (`'`), preventing spreadsheet formula execution upon opening exported CSVs in Microsoft Excel or Google Sheets.
- **UI Error Sanitization**: Error dialogs surface sanitized user-friendly descriptions rather than raw database file paths or internal exceptions.
- **Lifecycle & Cancellation Safety**: ViewModel coroutines catch and rethrow `CancellationException` to avoid swallowing structured concurrency cancellations, while ensuring saving state flags reset properly on failures to prevent UI deadlocks.

---

## Backup & Schema Format

Backups are exported as UTF-8 encoded JSON with schema version `1`:

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-10-01T10:00:00Z",
  "currency": "IDR",
  "categories": [
    { "id": 1, "name": "Food & Dining", "type": "EXPENSE", "color": 4294198070, "icon": "restaurant" }
  ],
  "transactions": [
    { "id": 1, "amount": 25000, "type": "EXPENSE", "categoryId": 1, "date": 20728, "note": "Lunch" }
  ],
  "recurringItems": [
    { "id": 1, "amount": 150000, "categoryId": 1, "frequency": "MONTHLY", "nextDueDate": 20758 }
  ]
}
```

Dates are represented as integer epoch days (`LocalDate.toEpochDay()`), and amounts are stored as whole-number 64-bit integers (`Long`).

---

## License

This project is licensed under the [MIT License](LICENSE).
