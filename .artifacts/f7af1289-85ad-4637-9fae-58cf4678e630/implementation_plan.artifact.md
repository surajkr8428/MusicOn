# Implementation Plan - Fix Header Spaces and App Sharing

The user wants to remove excessive vertical space in the app header (especially visible in landscape) and fix an issue where the shared APK appears as "invalid" when trying to install it.

## User Review Required

> [!IMPORTANT]
> **Split APK Issue**: The "App not installed as package appears to be invalid" error is most likely caused by the app being installed as a **Split APK** (common in Android Studio debug builds or Play Store installs). When sharing only the `base.apk`, it lacks necessary resources/code for a standalone installation. I will improve the sharing logic to use the cache directory and correct permissions, but if the app is a split install, the recipient may still face issues unless a "Universal APK" is built and shared.

## Proposed Changes

### UI Components & Screens

#### [DONE] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- Replaced `TopAppBar` with a more compact custom header for landscape.
- Removed `windowInsets` in the header.
- Reduced vertical padding in `LibraryTopBar` and `ScrollableTabRow`.

#### [MODIFY] [SettingsScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/SettingsScreen.kt)
- Apply compact header height and zero insets in landscape mode.

#### [MODIFY] [EqualizerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/EqualizerScreen.kt)
- Apply compact header height and zero insets in landscape mode.

#### [MODIFY] [Mp3CutterScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/Mp3CutterScreen.kt)
- Apply compact header height and zero insets in landscape mode.

#### [DONE] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- Updated `Share App (APK)` logic to use `context.cacheDir` and `applicationInfo.sourceDir`.
- Set `contentWindowInsets` to zero in the root `Scaffold`.

## Verification Plan

### Automated Tests
- Code compilation check for all modified screens.

### Manual Verification
- Verify all secondary screens (Settings, Equalizer, Cutter) have compact headers in landscape.
