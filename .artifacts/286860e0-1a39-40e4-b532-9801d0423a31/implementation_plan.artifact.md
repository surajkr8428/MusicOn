# Implementation Plan - Advanced Features, Stellar UI & Synchronized Lyrics

Comprehensive redesign to a single-screen drawer-based architecture, featuring local music scanning, deep search, circular player animations, and **synchronized lyrics**.

## User Review Required

> [!IMPORTANT]
> **Lyrics Integration**: Lyrics will be supported via the `.lrc` file format or embedded strings. If no lyrics are found, a placeholder will be shown. I will implement a scrolling animation that highlights the active line based on playback time.

> [!TIP]
> **Technical Metadata**: I will update the import logic to extract and display technical details like **Bitrate (kbps)** and **Duration** exactly as seen in the reference images.

## Proposed Changes

### 1. Navigation & Structural Redesign
- **Modal Drawer**: Move the "Folders" (Import/Sync) functionality into a left-side navigation drawer.
- **Top Actions**: Consolidate Playlist creation (+), Search, and Settings into the Library top bar.
- **Gesture Control**: Implement `BackHandler` in the Player screen for smooth exit using gestures or system back button.

---

### 2. Deep Search & Scanning
- **Universal Search**: Implement a real-time filter in `MainViewModel` that searches through Song Titles, Artist names, and Album names.
- **Local Scanner**: Implement a full device scan using `ContentResolver` to automatically populate the library with local audio files.

---

### 3. Detailed Metadata & UI
- **Track Metadata**: Update `MediaMetadataUtils` to extract **Bitrate** (`METADATA_KEY_BITRATE`) and ensure it's saved in `TrackEntity`.
- **UI Details**: Display "Artist | Duration | Bitrate" (e.g., "Parry Sidhu | 6:56 | 320k") in library items and bottom sheets.
- **Favorites**: Treat "Favorite" as the default playlist. The heart icon will instantly sync with this playlist.

---

### 4. Player & Lyrics Animation
- **Rotating Art**: Implement a smooth rotation animation for the circular thumbnail that runs during playback.
- **Synchronized Lyrics**:
    - **LRC Parser**: Create a utility to parse time-stamped lyrics.
    - **Lyrics UI**: A vertical scrolling list in the Player that centers and highlights (lavender color + scale) the line matching the current playback time.
    - **Animation**: Use `animateScrollToItem` to keep the active lyric line in focus.

---

### 5. Multi-Selection & Bulk Actions
- **Bulk Import**: Enable multi-select for both local file picker and GDrive listing.
- **Bulk Management**: Enable multi-select in the Library (via long-press) to perform bulk **Delete**, **Add to Playlist**, and **Upload**.

## Verification Plan

### Manual Verification
1. **Details**: Import a song -> Verify "320k" or similar bitrate is displayed in the list.
2. **Lyrics**: Play a song with a matching `.lrc` file -> Open Player -> Switch to "Lyrics" tab -> Verify lines scroll and highlight automatically.
3. **Drawer**: Open 3-line menu -> Click "Folders" -> Verify import hub appears correctly.
4. **Favorites**: Tap heart icon -> Verify song appears in the "Favorite" playlist and shows a heart in the main list.
5. **Animation**: Verify album art rotates during play and stops when paused.
