# Implementation Plan - Enhanced Cloud Browser & UI Refinement

This plan focuses on making the Cloud Browser a full-featured management tool, enabling multi-selection for sync operations, and further polishing the Player UI.

## Proposed Changes

### Cloud Storage & Management

#### [MODIFY] [CloudStorageManager.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/data/remote/CloudStorageManager.kt)
- Set the default folder ID to `14W_7EbfeM4oTwyXxS1FL7jt5Sf_6siCg` for all cloud operations.
- Add `deleteFile(fileId: String)` to support remote management.

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Cloud Browser Upgrade**:
    - Implement **Pull-to-Refresh** using `PullToRefreshBox`.
    - Implement **Multi-selection** for cloud files.
    - Replace individual download buttons with a **3-dot menu** containing Download, Rename, and Delete.
    - Add a **Bulk Action Bar** (top) that appears when items are selected.
    - Add a "Upload Local Songs" button in the Cloud Browser to select and upload any number of local tracks.
- **Renaming Support**:
    - Add a `RenameDialog` that works for both local and cloud songs.

### Player UI Refinement

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Big Central Sleep Timer**:
    - In landscape, position the sleep timer **exactly equidistant** between the playback control row and the song icon row.
    - Increase font size to `headlineLarge` for high visibility.
- **Duplicate Handling**:
    - Refine `SyncWorker` to use the fixed folder ID and strictly check for filename duplicates before starting an upload.

## Verification Plan

### Automated Tests
- Verify successful build.
- Check logs for "Duplicate detected" messages during sync.

### Manual Verification
- **Cloud Browser**:
    - Drag down to refresh.
    - Long-press or use checkboxes to select multiple songs.
    - Bulk download/delete.
    - Rename a cloud song and verify on Drive.
- **Player UI**:
    - Set a sleep timer and verify its big, bold, centered position in landscape.
- **Upload**:
    - Upload multiple selected songs from the local library.
