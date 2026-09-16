# Implementation Plan - "Raag" Final UI Polish & Feature Reliability

This plan focuses on finalizing the "Raag" branding, perfecting the header layout, and ensuring all playlist and track interaction features are high-fidelity and functional.

## User Review Required

> [!IMPORTANT]
> **Stretched Progress Bar**: I will adjust the `LibraryTopBar` to ensure the `SyncProgressBar` fills the entire gap between the app icon and the right-side actions, providing a more balanced and functional look.
> **Playlist Action Suite**: I will integrate the `PlaylistOptionsBottomSheet` into the library view, allowing users to rename, share, and delete playlists directly from the main screen.
> **Functional Track Metadata**: The "Info" icon in the track menu will now open a detailed technical dialog, and the "Share" icon will trigger the system share sheet for the audio file.

## Proposed Changes

### 1. Header Layout & Branding

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **TopBar Refinement**:
    - Ensure "Raag" and the icon are aligned.
    - Stretch the `SyncProgressBar` using `Modifier.weight(1f)` to span the width of the header.
- **Info Dialog**: Implement a professional `SongInfoDialog` that displays Title, Artist, Album, Bitrate, and File Path.

### 2. Advanced Playlist Controls

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Playlist Items**: add the "More" icon to both list and grid playlist items.
- **Action Handling**: map Rename, Share, and Delete actions from the bottom sheet to the `MainViewModel`.

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **Share Logic**: finalize the `sharePlaylist` and `shareTrack` functions to use `FileProvider`.

### 3. MiniPlayer Playback Controls

#### [MODIFY] [MiniPlayer.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/MiniPlayer.kt)
- **Triple-Button Support**: Add **Previous**, **Play/Pause**, and **Next** buttons to the player band.
- **Zero-Gap Refinement**: ensure the band width and padding are pixel-perfect when docked against the sidebars.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Header**: verify the progress bar is stretched across the top.
- **Playlists**: rename a playlist and verify the change persists.
- **Sharing**: click the share icon on a track and verify the system share sheet opens.
- **Metadata**: click the info icon and verify the technical details dialog appears.
- **MiniPlayer**: test all three control buttons for responsiveness.
