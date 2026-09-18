# Implementation Plan - "Nirvaana" Logo & Feature Completion

This plan completes the aesthetic rebranding to **Nirvaana**, implements essential playlist management tools, and fixes visual bugs in the progress bar and player.

## User Review Required

> [!IMPORTANT]
> **Aesthetic Logo**: I will integrate the redesigned minimalist 'N' logo into the launcher and splash screen.
> **Playlist Management**: I will add a `+` button inside playlists that opens a multi-select dialog to add songs from your library.
> **Progress Visibility**: I will increase the height and contrast of the progress bars in the header and MiniPlayer to ensure they are clearly visible.

## Proposed Changes

### 1. Brand Identity (Nirvaana)

#### [MODIFY] [ic_nirvaana_logo.xml](file:///D:/MusicOn/app/src/main/res/drawable/ic_nirvaana_logo.xml)
- Finalize the aesthetic design with soft blue/white curves.

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- Update branding text and icon sizes for the splash screen.

### 2. Functional Additions

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Add Songs logic**: implement a `MultiSelectSongDialog` and wire it to the `+` button in `PlaylistDetailScreen`.
- **Merged Indicator**: refine the cloud icon overlay logic to be non-obtrusive but clearly visible on merged local+cloud songs.

### 3. Visual Bug Fixes

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **SyncProgressBar**: increase visibility by using Material 3 `LinearProgressIndicator` instead of custom Canvas drawing if possible, or increase the line thickness.

#### [MODIFY] [MiniPlayer.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/MiniPlayer.kt)
- Increase the height of the bottom red progress line to `4.dp` for better visibility.

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Rotation Fix**: ensure the `rotation` animation correctly reflects the `isPlaying` state of the media player without resetting to zero every time.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Playlist Add**: open a playlist, click `+`, select 2 songs. Verify they are added.
- **Merged Icon**: check a song that is both local and synced to GDrive. Verify top-right cloud icon.
- **Progress Visibility**: verify both progress bars (header and MiniPlayer) are clearly visible during sync/play.
- **Player UI**: confirm the image rotates when playing and pauses when paused.
