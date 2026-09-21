# Implementation Plan - Nirvaana UX Final Polish & Stability

This plan addresses the persistent crash, restores the missing player queue, fixes the APK sharing logic, and ensures the playback state (playlist memory) is correctly restored on app launch.

## User Review Required

> [!IMPORTANT]
> **Crash Fix (State Stability)**: I am simplifying the `PagerState` and its dependency on the `tabs` list to prevent internal method resolution errors.
> **Persistent Player Queue**: The bottom song icons in the player were being pushed off-screen due to the scrollable layout. I am splitting the layout so the artwork scrolls but the **song icons remain pinned at the bottom**.
> **Playlist Memory (Restore State)**: Refining the startup logic to ensure the last played track and position are correctly loaded into the player once the media library is ready.
> **Robust APK Sharing**: Re-engineering the sharing intent to correctly handle the APK file preparation on a background thread and providing proper permissions to the sharing sheet.

## Proposed Changes

### 1. Stability & Build Fixes

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- Use a simplified `rememberPagerState` initialization to avoid lambda resolution issues on different devices.

### 2. Player UI & Gestures

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Queue Visibility**: Remove `verticalScroll` from the main container of `PlayerLayoutPortrait`. Instead, use a `Column` where the Artwork/Controls take `weight(1f)` and the Queue is placed at the bottom with a fixed height.
- **Swipe Sensitivity**: Adjust horizontal drag threshold for track skipping.

### 3. Startup & Memory Fixes

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **APK Sharing**: move to `viewModelScope.launch(Dispatchers.IO)` and ensure the chooser intent has `FLAG_ACTIVITY_NEW_TASK`.
- **State Restoration**: verify `playbackQueue` and `currentPlayingTrack` are populated correctly from `SettingsRepository` values during `init`.

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- Add defensive checks to state restoration in `LaunchedEffect(mediaController)`.

### 4. UI Refinement

#### [MODIFY] [TechnicalInfoPopup.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/TechnicalInfoPopup.kt)
- Make Artist and Album even more prominent in the info display.

## Verification Plan

### Automated Tests
- Build verification: `gradle clean app:assembleDebug`.

### Manual Verification
- **No Crash**: Verify library browsing on mobile device.
- **Queue Check**: verify song icons are always visible at the bottom of the player.
- **Memory Check**: Close app completely, relaunch, and verify same song and position are restored.
- **Sharing**: click "Share APK" and verify the sharing sheet appears with the file.
