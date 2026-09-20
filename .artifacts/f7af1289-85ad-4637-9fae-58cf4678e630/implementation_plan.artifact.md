# Implementation Plan - Nirvaana UX Final Polish & Persistence

This plan addresses font consistency, playlist persistence, cloud management improvements, and restores missing player UI components.

## User Review Required

> [!IMPORTANT]
> **Global Cursive Identity**: All text across the app (Settings, Lists, Sidebar, etc.) will now use the premium `FontFamily.Cursive` style to match the brand identity.
> **Immortal Playlists**: Refined the storage logic to ensure that even if local files are temporarily missing (e.g. unmounted SD card), synced songs are **never removed from the database**. This guarantees your playlists remain intact across restarts and updates.
> **Cloud Wipe**: Adding a new feature in the Cloud Browser to **Delete All Cloud Songs** in one tap.
> **Dynamic Sync Toggle**: The Sync button in the sidebar will now transform into a **Pause/Stop icon** while an active sync is running.

## Proposed Changes

### 1. Brand Identity (Universal Typography)

#### [MODIFY] [Type.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/theme/Type.kt)
- Update all `TextStyle` definitions (`bodyLarge`, `labelSmall`, etc.) to use `fontFamily = FontFamily.Cursive`.

### 2. Storage Persistence Mastery

#### [MODIFY] [MusicRepository.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/data/MusicRepository.kt)
- **Safe Cleanup**: Update `scanLocalStorage()` to skip deleting tracks if `existingTrack.gDriveId != null`. This preserves the song entity (and its playlist entries) even if the local file is missing.

### 3. User Interface Fixes & Regressions

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Restore Queue**: Re-integrate the `LazyRow` bottom queue into `PlayerLayoutPortrait` and `PlayerLayoutLandscape`.

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Flexible Sidebar Email**: Apply `fontSize = 14.sp` and `maxLines = 1` with `overflow = TextOverflow.Ellipsis` to the sidebar email display.

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Smart Grouping**: Update `GroupedTab` (Albums) to group by `Album + Artist` to avoid merging different albums with the same title.
- **Workflow Persistence**: Ensure `isAddingTracks` flag correctly returns the user to the `PlaylistDetailScreen`.

### 4. Cloud & Sync Polish

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **Bulk Cloud Delete**: Implement `deleteAllCloudTracks()` using WorkManager or a direct coroutine call.
- **Sync Toggle**: Add logic to cancel active WorkManager sync tasks when the "Stop" button is clicked.

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt) (Sidebar)
- Update Sync button icon based on `syncStatus`:
    - `Idle` / `Success` / `Error` -> `Icons.Default.Sync`
    - `Loading` -> `Icons.Default.Stop` (to cancel)

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Font Check**: verify Settings and song lists now use the Cursive font.
- **Persistence Test**: Manually rename a music folder on the device, run a scan, and verify synced songs stay in the library/playlists.
- **Cloud Wipe**: Use the "Delete All" button in Cloud Browser and verify Drive is cleared.
- **Sync Control**: Start a sync and verify the button changes to a Stop icon; click it to cancel.
- **Player Recovery**: confirmed song icons are back at the bottom of the player.
