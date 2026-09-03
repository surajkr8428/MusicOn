# Implementation Plan - Metadata, Sync, and Filtering Fixes

This plan addresses issues with missing song images, sync failures, and unwanted call recordings in the library.

## Proposed Changes

### Core Logic & Metadata

#### [MODIFY] [MediaMetadataUtils.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/logic/MediaMetadataUtils.kt)
- Enhance image extraction:
    - First, try `embeddedPicture` (already implemented).
    - Second, if not found, look for "cover.jpg" or "album.jpg" in the same directory as the song file.
    - Third, use a default fallback if all else fails.

#### [MODIFY] [MusicRepository.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/data/MusicRepository.kt)
- **Aggressive Filtering**: Update the `scanLocalStorage` query to explicitly exclude directories like "CallRecordings", "Recorder", and files containing "call" in their path.
- **Robust Duplicates**:
    - Use `localPath` as the primary key for local files during scanning.
    - Before adding a cloud track, check if a track with the same title and artist already exists to prevent duplicate entries for the same song across local/cloud.
- **Sync Logging**: Add more detailed logging to `syncCloudTracks` to identify where it fails (auth, network, or data parsing).

### UI Refinement

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- Ensure image loading in `StellarTrackItem` and `StellarGridItem` has proper error handling and placeholder display.

## Verification Plan

### Automated Tests
- Verify successful build.
- Log check: Ensure "Skipping duplicate" and "Filtering out recording" logs appear correctly.

### Manual Verification
- **Images**: Browse the library and verify that more songs now show their album art.
- **Filtering**: Check "All Songs" to ensure no call recordings are present.
- **Duplicates**: Run a scan and cloud sync; verify no duplicate songs appear for those already in the library.
- **Sync**: Check the Cloud Sync status message for success or specific error details.
