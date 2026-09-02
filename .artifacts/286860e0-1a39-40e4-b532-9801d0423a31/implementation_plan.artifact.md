# Implementation Plan - Comprehensive UI Refinement & Interaction

I will resolve the layout issues, implement the enhanced sorting system, add dynamic view modes to all tabs, and fix the APK sharing system. I will also add a dedicated Sleep Timer display to both the Library and Player screens.

## User Review Required

> [!IMPORTANT]
> - **Landscape Header**: I am reducing the top bar height in landscape mode to **48dp** and decreasing the padding to maximize content space on phones.
> - **Player Timer Message**: If a Sleep Timer is active, a message (e.g., "Music stops in 14m") will be displayed as a subtle badge in the Player UI.
> - **Dynamic Sorting**: The Sorting menu will offer "Increase/Decrease" and "A-Z/Z-A" options in a compact, low-padding list.
> - **Adaptive Grid**: I will use `GridCells.Adaptive` to ensure the Grid view works perfectly on tablets and large-screen phones.

## Proposed Changes

### Adaptive & Responsive UI
#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Compact Landscape Header**: Detect orientation and shrink the `TopAppBar` height and text size when rotated.
- **Top Sleep Timer**: Add a high-visibility badge next to the title.
- **Universal View Mode**: Apply the List/Grid toggle to **Playlists**, **Albums**, **Artists**, and **Genres** tabs.
- **Grid Density**: Switch to `GridCells.Adaptive(minSize = 100.dp)` to make thumbnails smaller and more high-density.
- **Enhanced Sorting**:
    - Redesign `SortMenu` to be compact with zero wasted space.
    - Add sub-options for Descending/Ascending (A-Z vs Z-A).

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Sleep Timer Message**: Add a `Surface` badge at the top of the player content that says "Sleep Timer: XXm remaining."
- **Landscape Refinement**: Adjust split-screen weights to ensure lyrics and controls have optimal spacing.

### Core Fixes & Sharing
#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **APK Share Fix**:
    - Copy APK to `externalCacheDir` for better permission handling.
    - Use `ClipData` on the Intent to ensure WhatsApp and other apps can read the file.
- **Startup Animation**: Fine-tune the scale/fade duration for a snappier feel.

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- Update `filteredTracks` to support complex sort orders (e.g., `NAME_ASC`, `NAME_DESC`).

## Verification Plan

### Manual Verification
1.  **Landscape Spacing**: Rotate the phone. Verify the top bar is significantly smaller and content fills the screen.
2.  **Sorting**: Verify "Name Z-A" and "Duration (Shortest first)" work correctly.
3.  **Player Timer**: Set a timer and verify the message appears at the top of the Player screen.
4.  **Tab Device**: Test the List/Grid toggle on a tablet profile to ensure the adaptive grid fills the width.
5.  **Share Fix**: Share the APK and verify it installs on a second device.
