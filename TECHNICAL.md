# Finance Tracker: Architecture and Development

Technical documentation covering architecture, build setup, testing, and security.

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

Finance Tracker follows a single-activity architecture built with Android Jetpack libraries:

- Presentation: Jetpack Compose (Material 3) with unidirectional data flow (UDF). Screens observe immutable UI state flows from ViewModels.
- Persistence: Room SQLite database with Flow-based DAOs and transactional operations.
- Navigation: Navigation Compose with a four-tab bottom navigation bar (`Dashboard`, `Add`, `History`, `Categories`) and sub-routes for reminders, export, and backup.
- Charts: Vico for cashflow bars and spending trend lines.
- Serialization: Kotlinx Serialization for JSON backup files.
- Reminders: `AlarmManager` with `BroadcastReceiver` to post notifications on scheduled due dates across device reboots.

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
│   │   ├── test/                                 # Unit test suites covering ViewModels, DAOs, and utilities
│   │   └── androidTest/                          # Compose UI and Room instrumentation tests
│   └── build.gradle.kts                          # Module build script and dependency declarations
├── gradle/
│   ├── libs.versions.toml                        # Version catalog (dependencies and plugins)
│   └── wrapper/                                  # Gradle wrapper 9.3.1
└── settings.gradle.kts
```

---

## Prerequisites & Toolchain

- **JDK**: Java Development Kit 21 or higher (Android Studio bundled JBR works out of the box).
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

Gradle outputs the APK to `app/build/outputs/apk/debug/app-debug.apk`.

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

Run the unit test suite covering ViewModels, Room DAOs, backup encoding, reminders, and formatters (see the CI badge above for latest status):

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

Instrumented tests cover UI interactions, process death restoration, and database queries on an emulator or device (API 26+):

```bash
# macOS / Linux
./gradlew :app:connectedDebugAndroidTest

# Windows (PowerShell)
.\gradlew.bat :app:connectedDebugAndroidTest
```

---

## Data Integrity & Security Safeguards

- Zero network permissions: The manifest omits `android.permission.INTERNET`. The app cannot open network sockets.
- Backup validation: `BackupValidator` checks all JSON payloads before database insertion, enforcing positive amounts (`amount > 0`), valid category associations, non-empty names, and field length limits (`MAX_NOTE_LENGTH = 100`, `MAX_CATEGORY_NAME_LENGTH = 36`).
- CSV formula injection protection: In `Exporter.kt`, values starting with formula characters (`=`, `+`, `-`, `@`) or tab characters are prefixed with a single quote (`'`) to neutralize spreadsheet formula execution.
- User-facing error handling: Error dialogs show sanitized messages rather than raw database paths or stack traces.
- Coroutine lifecycle safety: ViewModel coroutines rethrow `CancellationException` to preserve structured concurrency and reset in-flight state flags on failures.

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
