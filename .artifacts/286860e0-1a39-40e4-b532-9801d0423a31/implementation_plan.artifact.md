# Implementation Plan - Dynamic Theming, Adaptive UI, Sorting & Gesture Refinement

I will implement a dynamic "Auto Theme Color" feature, optimize the app for both orientations, add a comprehensive sorting system, and refine the swipe gestures for absolute precision.

## User Review Required

> [!IMPORTANT]
> - **Auto Theme Color**: The app will extract colors from album artwork and apply them to the UI theme in real-time.
> - **Landscape Support**: Split-screen player and grid-based library for a professional landscape experience.
> - **Precise Swiping**: I will update the swipe-to-skip logic to ensure that a single horizontal swipe **only changes the track once**. You won't accidentally skip multiple songs with one long movement.
> - **Library Sorting**: A new sort icon will be added to the library top bar for Name, Artist, Date Added, and Duration.
> - **Shuffle Toggle Fix**: I will resolve the bug where toggling shuffle/unshuffle causes track skipping.

## Proposed Changes

### Dynamic Theming
#### [MODIFY] [SettingsRepository.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/data/SettingsRepository.kt)
- Add `AUTO_THEME` boolean key.

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- Add `extractedAccentColor` StateFlow.
- Add `sortOrder` StateFlow for each library tab.

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- Integrate **Palette API** to extract colors from `currentTrack` artwork.
- Update `MusicOnTheme` to prioritize `extractedAccentColor` if `autoTheme` is enabled.

### Adaptive UI & Gestures
#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Single-Skip Gesture**: Use a state flag to ensure `player.seekToNext()` or `seekToPrevious()` is called only once per horizontal drag session.
- **Landscape View**: Artwork (Left), Controls & Info (Right).
- **Lyrics**: Enhanced centered animation with gradient fading.

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- Multi-column grid layout for landscape.
- Add "Sort" button to the `LibraryTopBar`.

### Settings Screen
#### [MODIFY] [SettingsScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/SettingsScreen.kt)
- Add **"Auto Theme Color"** toggle under theming.
- Add "Audio Quality" and "Sleep Timer" options.

## Verification Plan

### Manual Verification
1.  **Swipe Test**: Perform a long swipe in the player. Verify only one song change occurs.
2.  **Sorting**: Verify "All Songs" correctly reorders when changing sort criteria.
3.  **Auto Theme**: verify that when playing a song with a dominant red cover, the buttons turn red.
4.  **Rotation**: Verify the player looks professional in landscape.
