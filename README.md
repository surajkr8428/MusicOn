# MusicOn

MusicOn is an Android music player built with Jetpack Compose, Android Media3, Room, WorkManager, and Google Drive integration.

## Overview

This repository contains an Android application that provides:

- Local music playback with a modern Compose UI
- Playlist creation and management
- Background audio playback support
- Persistent local storage using Room
- Sync and cloud integration using Google Drive
- Settings and preferences stored with DataStore
- Media metadata handling and lyrics parsing

## Tech stack

- Kotlin
- Android Jetpack Compose
- Android Media3
- Room persistence library
- WorkManager
- Google Drive API
- Gradle Kotlin DSL

## Getting started

### Prerequisites

- Android Studio
- Android SDK with API level 37
- Java 11
- A Google account for Drive integration

### Build and run

1. Open the project in Android Studio.
2. Configure `local.properties` with your Android SDK path if it is not already set.
3. Sync Gradle.
4. Run the `app` module on an Android device or emulator.

Alternatively, from the command line:

```bash
cd MusicOn
./gradlew assembleDebug
```

On Windows:

```powershell
cd D:\MusicOn
.\gradlew.bat assembleDebug
```

## Project structure

- `app/` - Android application module
  - `src/main/java/com/example/musicon` - app source code
  - `src/main/res` - UI resources and assets
  - `build.gradle.kts` - module Gradle configuration
- `gradle/` - Gradle wrapper files
- `build.gradle.kts` - root Gradle configuration
- `settings.gradle.kts` - project settings
- `.gitignore` - ignored files and folders

## Notes

- `local.properties` is excluded from version control and should contain your local Android SDK path.
- The Google Drive integration requires API credentials and consent configuration.

## Contributing

If you want to contribute:

1. Fork the repository.
2. Create a new branch for your changes.
3. Submit a pull request with a clear description of the changes.

## License

This project does not include a license file. Add one if you want to publish or share the repository publicly.
