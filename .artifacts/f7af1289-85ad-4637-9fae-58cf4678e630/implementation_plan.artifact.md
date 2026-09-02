# Implementation Plan - UI Overlap, Big Sleep Timer, and Cloud Fixes

This plan fixes the header overlap, overhauls the sleep timer (size, format, and landscape position), increases icon sizes, and resolves cloud functionality issues.

## Proposed Changes

### Media & Service

#### [MODIFY] [PlaybackService.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/service/PlaybackService.kt)
- (Finalize) Link media notification to `MainActivity` so tapping it opens the app.

#### [MODIFY] [SyncWorker.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/service/SyncWorker.kt)
- (Finalize) Remove hardcoded folder ID for cloud uploads.

### UI Components & Screens

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Fix Header Overlap**: Update the root `Scaffold` to use `contentWindowInsets = WindowInsets.statusBars`. This ensures headers start *below* the system status bar.
- **Sidebar Actions**: Implement navigation for the new Cloud menu items.

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Big Sleep Timer Overhaul**:
    - Add `formatSleepTime(Long): String` for `HH:MM:SS` format.
    - **Position**: In landscape, move the sleep timer to be **just below the play/pause button row**.
    - **Style**: Use a **Bold and Big** font (`headlineSmall`, matching the song name).
    - Ensure it is visible and correctly aligned in both orientations.
- **Bigger Song Icons**:
    - Increase landscape queue icon size to **56dp**.
    - Increase `LazyRow` height to **72dp** for a better fit.

### Cloud Logic Investigation

#### [MODIFY] [data/remote/CloudStorageManager.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/data/remote/CloudStorageManager.kt)
- Add comprehensive logging to the upload process to track failures.

## Verification Plan

### Automated Tests
- Build verification.

### Manual Verification
- **Header**: Confirm no overlap with the status bar.
- **Sleep Timer**: Verify it's **Big, Bold, HH:MM:SS**, and sits **below the Play button** in landscape.
- **Queue Icons**: Confirm they are significantly larger (56dp) in landscape.
- **Cloud**: Perform an upload and check logs for success.
