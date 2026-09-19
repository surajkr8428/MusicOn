# Implementation Plan - Nirvaana UX Mastery & Aesthetic Iconography

This plan provides a major visual and functional update to **Nirvaana**, focusing on standardizing song item layouts, enabling rich inline exploration, and refreshing the brand identity with adaptive sidebars and premium progress tracking.

## User Review Required

> [!IMPORTANT]
> **Headphone + N Branding**: I am redesigning the logo to feature a minimalist headphone silhouette with a stylized 'N' at its core. Any non-aesthetic background colors (like green) will be removed.
> **Giant Sidebar Progress**: I am doubling the size of the circular progress bar in the sidebar (`240.dp`) and ensuring it always displays the "X / Y Songs Uploaded" status at its center.
> **Advanced Inline Expansion**: Playlists, Albums, Artists, and Genres will all expand inline. The technical info ("i") panel for each song will now prominently display the **Album and Artist** name alongside technical specs.
> **Massive Artwork UI**: I am significantly increasing the size of the rotating disc and square artwork in the Player UI. For songs missing images, a **giant, high-fidelity Music Note** will be used.
> **Curated Iconography**: Every category (Albums, Artists, Genres) and every individual playlist will feature a unique, aesthetic music-themed icon.

## Proposed Changes

### 1. Brand Identity (Nirvaana 2.0)

#### [MODIFY] [ic_nirvaana_logo.xml](file:///D:/MusicOn/app/src/main/res/drawable/ic_nirvaana_logo.xml)
- Redesign with "Headphone + N" aesthetic. Ultra-minimalist blue/white theme.

### 2. Adaptive Sidebar & Giant Progress

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- Calculate sidebar widths based on screen percentage (`0.85f` of width) for a perfect fit.

#### [MODIFY] [CircularSyncProgressBar.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/CircularSyncProgressBar.kt)
- **Size**: Increase diameter to `240.dp`.
- **Always-On Status**: Logic to ensure the circle remains visible with a "Ready" or "Syncing" state and current upload counts.

### 3. Tab Hierarchy & High-Fidelity Layout

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Order**: `Recent, All Songs, Albums, Artists, Genres`.
- **Default Tab**: "All Songs".
- **Unique Icons**:
    - Specific aesthetic icons for the 5 main tabs.
    - Dynamic unique icons for every playlist item.
- **Song Item Standard**:
    - **Cloud Icon**: Top-Left.
    - **Menu (3-dots)**: Top-Right.
    - **Info (i)**: Bottom-Right.
- **Metadata Expansion**: Update the "i" info panel to show: `Album: [Name]` and `Artist: [Name]` in a bold, readable layout.

### 4. Massive Player UI

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Scale Up**: Increase the artwork container size (Disc and Square) by ~30% for a "hero" feel.
- **Giant Note Fallback**: Replace the default placeholder with a massive, high-contrast Music Note icon that rotates at 60fps when in `ROTATION` mode.

### 5. Logic & Stability

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **All Songs**: ensure it pulls every song from local storage without exception.
- **Recents**: implement session-based tracking (songs played since app startup).

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Branding**: Verify new "Headphone + N" logo in launcher and headers.
- **Giant Progress**: Open drawer and verify the 240dp circle looks massive and professional.
- **Expansion**: Click a Playlist and verify inline expansion with Album/Artist details in the info sheet.
- **Player Sizing**: Confirm the rotating disc takes up more screen space and the giant note appears for empty art.
- **Icon Variety**: verify each tab and playlist has a distinct, aesthetic icon.
