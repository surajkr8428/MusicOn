# Implementation Plan - App Icon, MiniPlayer Controls & UI Refinement

This plan covers the app icon replacement, adding playback controls to the MiniPlayer, and repositioning the progress bar for better accessibility in portrait mode.

## User Review Required

> [!IMPORTANT]
> **App Icon**: I will set the provided image as the new primary app icon. This will replace the default Android icon on your home screen and in the app drawer.
> **MiniPlayer Controls**: I will add Previous, Play/Pause, and Next buttons to the bottom player band. This will make it slightly taller or more densely packed to accommodate the new controls.
> **UI Cleanup**: I will remove the "Shuffle" and "Play" buttons from the top of the library tabs to clean up the header space.

## Proposed Changes

### 1. App Icon Replacement
- **Provided Image**: I will save the provided image to `res/mipmap-xxxhdpi/ic_launcher.png` (and other densities) and update the `AndroidManifest.xml` to ensure it's used as the icon.

### 2. MiniPlayer Enhancement

#### [MODIFY] [MiniPlayer.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/MiniPlayer.kt)
- **New Buttons**:
    - Add an `IconButton` for **Skip Previous** (left of song info).
    - Maintain the **Play/Pause** button.
    - Add an `IconButton` for **Skip Next** (right of play button).
- **Layout**: Use a more compact `Row` structure to fit all controls while maintaining readability.

### 3. Progress Bar & Header Refactor

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Relocation**: In `LibraryTopBar`, move the `SyncProgressBar` from the row to a new line directly below the "MusicOn" title and status pills.
- **Removal**: Delete the `StellarActionButton` row (Shuffle/Play) from the `SongsTab` to streamline the library view.

### 4. Logic & Persistence

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **Persistence**: Ensure the `lastTrackId` is properly loaded on startup and that the playback queue is initialized so that pressing "Play" in the MiniPlayer works immediately after a fresh launch.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **App Icon**: check the emulator/device home screen for the new icon.
- **MiniPlayer**: verify all three buttons (Prev, Play/Pause, Next) work as expected.
- **Progress Bar**: verify it appears below the title in portrait mode.
- **Tabs**: verify the "Shuffle" and "Play All" buttons are gone.
- **Persistence**: Play a song, force close the app, reopen, and verify the song is still loaded in the MiniPlayer.
