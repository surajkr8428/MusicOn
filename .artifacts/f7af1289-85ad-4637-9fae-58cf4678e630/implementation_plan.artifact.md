# Implementation Plan - Multi-select, Big Sync Progress, and MiniPlayer Enhancements

This plan addresses several UI and functional improvements: Select All songs feature, a prominent cloud sync progress bar, and adding song duration and sleep timer to the minimized player. It also incorporates previous fixes for metadata and filtering.

## Proposed Changes

### UI Components & Screens

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Select All Feature**:
    - Update `SelectionTopBar` to include a "Select All" icon button.
    - Passing the full list of currently visible tracks to `SelectionTopBar` to toggle selection.
- **Filtering**:
    - Update `SongsTab` to ensure it only shows non-call recordings (handled in repository).

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Big Sync Progress**:
    - Overhaul the `Sync Progress Indicator` inside the root `Scaffold`.
    - Increase font size for status messages.
    - Increase the height of the `LinearProgressIndicator` (e.g., `Modifier.height(8.dp)`).
    - Use a more distinct background color and adding shadow/elevation to make it "pop".

#### [MODIFY] [MiniPlayer.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/MiniPlayer.kt)
- **Time & Sleep Timer**:
    - Add a `sleepTimerRemaining` state using `viewModel.sleepTimerRemaining.collectAsState()`.
    - Display the current position and total duration (e.g., `01:23 / 04:56`) below the artist name.
    - Display the sleep timer (HH:MM:SS) in a small badge or next to the time info if active.

### Core Logic & Metadata

#### [MODIFY] [MediaMetadataUtils.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/logic/MediaMetadataUtils.kt)
- Improve album art extraction by checking for local `cover.jpg` or `album.jpg` files if embedded artwork is missing.

#### [MODIFY] [MusicRepository.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/data/MusicRepository.kt)
- Implement aggressive filtering in `scanLocalStorage` to exclude paths containing "Recorder", "CallRecordings", or "call".
- Ensure duplicate protection during scan by checking normalized file names and paths.

## Verification Plan

### Automated Tests
- Verify successful compilation and build.

### Manual Verification
- **Multi-select**: Go to library, long press a song, then tap the "Select All" button in the top bar. Verify all songs are checked.
- **Sync Progress**: Upload a large song and verify the new big progress bar is highly visible.
- **MiniPlayer**: Play a song and verify the time (e.g., 0:45 / 3:12) and the sleep timer (if set) appear on the player band.
- **Filtering**: Verify no call recordings appear in the list after a re-scan.
