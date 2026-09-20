# Implementation Plan - Nirvaana UX Mastery & Immortal Sync

This plan delivers the final tier of UI responsiveness, a robust Pause/Resume sync system, and the "Immortal Library" feature that preserves your settings and playlists even after an app reinstall.

## User Review Required

> [!IMPORTANT]
> **Immortal Library (Cloud Backup)**: To remember your settings and playlists after a full app reinstall, Nirvaana will now automatically sync a small `backup.json` to your Google Drive. On a fresh install, simply sign in, and your playlists will be restored instantly.
> **Advanced Sync Control**:
> - The Sync button now supports **Pause/Resume**.
> - Clicking "Pause" will suspend the current upload batch.
> - Clicking "Resume" will pick up exactly where it left off.
> **Responsive Player UI**: Fixed a layout bug where song icons were missing on some screen sizes by implementing a `Weighted Bottom Sheet` strategy for the player controls.
> **Compact Info Pop-up**: Re-engineered the technical specification window to be a modern, centered modal that adapts its size perfectly to the content.

## Proposed Changes

### 1. Advanced Sync Engine (Pause/Resume)

#### [MODIFY] [SyncStatus.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/data/remote/SyncStatus.kt)
- Add a `Paused` state to the `SyncStatus` sealed class.

#### [MODIFY] [SyncWorker.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/service/SyncWorker.kt)
- Implement a loop check: Before each file upload, check `CloudSyncManager.isPaused`. If true, the worker will enter a `while(paused) { delay(1000) }` state.

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- Implement `pauseSync()` and `resumeSync()` methods that update the `CloudSyncManager` state.

### 2. Immortal Library (Persistence across Reinstall)

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **Backup Logic**: Create `syncPlaylistsToCloud()`. This will serialize playlist names and their song titles into a JSON file on Google Drive.
- **Restore Logic**: On app startup, if signed in, check for `nirvaana_backup.json` and recreate playlists by matching song titles in the local library.

### 3. Responsive UI Fixes

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- Refactor the `Column` layout in `PlayerLayoutPortrait`.
- Ensure the `LazyRow` (song queue) is wrapped in a `Box` with a specific `height` or `weight` to prevent it from being pushed off-screen by the large artwork.

#### [MODIFY] [TechnicalInfoPopup.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/TechnicalInfoPopup.kt)
- Reduce internal padding and icon size to make the pop-up compact.
- Ensure `Cursive` font is applied to every text node.

### 4. Sidebar Dashboard Polish

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- Update the Sidebar Sync button logic to cycle through: `Sync` -> `Pause` -> `Resume` based on the current `SyncStatus`.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Reinstall Persistence**:
    1. Create a playlist.
    2. Uninstall and Reinstall the app.
    3. Sign in to Google.
    4. Verify the playlist is restored.
- **Sync Control**: Start a bulk upload, hit "Pause", verify it stops, then hit "Resume" and verify it continues.
- **Player UI**: confirm the bottom icons are visible on both small and large phone screens.
- **Pop-up**: verify the compact design of the technical info window.
