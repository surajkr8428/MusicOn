# Implementation Plan - Nirvaana UX Mastery & Massive Scale-Up

This plan delivers a comprehensive visual and functional scale-up for **Nirvaana**, focusing on universal grid accessibility, massive storage stats, and a smarter cloud-integrated library.

## User Review Required

> [!IMPORTANT]
> **Massive Typography**: I am scaling up the "Nirvaana Storage" title by **5x** and the Local/Cloud counts by **3x**. This will make the storage dashboard the dominant feature of the sidebar.
> **Universal Grid & Matrix**: All tabs (Recents, All Songs, etc.) will now respect the global List/Grid toggle. Expanded categories will use a "Flexible Matrix" (Staggered/Adaptive Grid) to display tracks inline.
> **Cloud Deletion**: I am adding a **"Delete from Cloud"** option to the song menu. This will allow you to permanently remove files from your Google Drive directly from the app.
> **Smart Playlist Building**: Clicking the `+` button in a playlist will now intelligently switch you to the "All Songs" tab and **activate Multi-Select mode** automatically, allowing for bulk additions.
> **Aesthetic Fallbacks**: Songs without artwork will now feature a high-fidelity Music Note image in the **Song Info Popup** and the **Player UI**.

## Proposed Changes

### 1. Adaptive Sidebar & Giant Dashboard

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- Scale typography in the Sidebar Storage section:
    - Title: `fontSize = 64.sp` (~5x increase).
    - Counts: `fontSize = 36.sp` (~3x increase).
- Ensure the layout remains balanced with these massive sizes.

#### [MODIFY] [CircularSyncProgressBar.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/CircularSyncProgressBar.kt)
- Add a **secondary Sync Button** at the center of the giant progress circle. This button will be clickable even when sync is idle.

### 2. Universal Library Mastery

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Universal Grid**: Refactor `SongsTab` and `GroupedTab` to use `LazyVerticalGrid` whenever `viewMode` is set to `GRID`, across all tabs (including Recents).
- **Flexible Matrix**: Refactor expanded Category views (inside Artists, Albums, etc.) to use an adaptive flow layout/grid instead of a vertical list.
- **Multi-Select Trigger**: Update `onAddSongs` to navigate to index `1` and set a flag in `MainViewModel` to force-activate selection mode.

### 3. Cloud Song Management

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **Delete from Cloud**: Implement `deleteTrackFromCloud(trackId)` which calls `CloudStorageManager.deleteFile` and updates the local entity to remove the `gDriveId`.
- **Selection State**: Add a `isForceSelectionMode` state to control the "Add Songs" workflow.

#### [MODIFY] [TrackOptionsBottomSheet.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/TrackOptionsBottomSheet.kt)
- Add "Delete from GDrive" option (only visible if `gDriveId` is not null).

### 4. High-Fidelity Artwork Fallbacks

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- Ensure the `MusicNote` fallback is aesthetically integrated into the "hero" artwork area.

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- Update `TechnicalInfoPopup` to include a beautiful large Music Note image for songs missing album art.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Sidebar Stats**: verify the massive font sizes look professional and readable.
- **Central Sync**: click the center of the 240dp progress circle to trigger a sync.
- **Matrix Expansion**: expand an Artist and verify tracks appear in a flexible grid/matrix form.
- **Cloud Delete**: verify a synced song can be removed from Drive via the song options menu.
- **Bulk Add**: verify clicking `+` in a playlist takes you to All Songs with selection mode already active.
