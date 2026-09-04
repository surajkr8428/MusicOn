# Implementation Plan - Theming, Connectivity Status, and Fixes

This plan addresses the unified header status, Dark/Light mode support, fixing the sign-in crash, and securing cloud access.

## Proposed Changes

### Core Fixes

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Fix Sign-in Crash**: Move `signInLauncher` registration to the class property level to avoid registration-during-method-call errors.
- **Header Structure**: Ensure the status pill is correctly positioned between the title and search icon by providing sufficient space in the `LibraryTopBar`.

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **Cloud Security**: Exclude cloud tracks from search results if `isUserSignedIn` is false.

### UI Components

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Status Pill (Header)**:
    - Implement a `HeaderStatusPill` component with `RoundedCornerShape(22.dp)` (matches Shuffle button).
    - Combine **Online/Offline** status and **Cloud Sync progress** into this pill.
    - Position it between the "MusicOn" title and the search icon.
- **Cleanup**: Remove the separate large online/offline banners.

#### [MODIFY] [SettingsScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/SettingsScreen.kt)
- **Theme Selection**: Add a settings item to toggle between **Light**, **Dark (Spotify)**, and **System** themes.

#### [MODIFY] [Theme.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/theme/Theme.kt)
- **Optimize Light Mode**: Refine `lightColorScheme` with soft neutral backgrounds and black text for a professional look.

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Cloud Privacy**: Update `CloudBrowserScreen` to show a "Sign in to view Cloud songs" empty state if the user is not authenticated.

## Verification Plan

### Automated Tests
- Build success check: `gradle app:assembleDebug`.

### Manual Verification
- **Sign-in**: Verify that clicking sign-in no longer closes the app and opens the Google account picker.
- **Theme**: Verify switching to Light Mode updates the entire app's aesthetic.
- **Status Pill**: Verify the compact ONLINE/OFFLINE indicator in the header.
- **Cloud Privacy**: Sign out and confirm cloud songs disappear from Library and Cloud Browser.
