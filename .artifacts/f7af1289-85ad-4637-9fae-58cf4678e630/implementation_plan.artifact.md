# Implementation Plan - Stability, Navigation & Timer Controls

This plan addresses the playback issue, refactors Settings to a sidebar-only model, enhances the sleep timer controls, and fixes cloud synchronization and visuals.

## User Review Required

> [!IMPORTANT]
> **Playback Reliability**: I will refine the `PlaybackService` to ensure it handles auth tokens more gracefully and uses a more reliable `MediaSource` for cloud tracks.
> **Timer Placement**: As requested, the sleep timer will be removed from the Player header and relocated into the main controls area with new **Pause** and **Reset** buttons.
> **Settings Access**: "Settings" will be removed from the left navigation drawer. It will only be accessible via the gear icon in the library header, opening in a sliding side panel.

## Proposed Changes

### Core Stability & Playback

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Fix Playback Flow**: Ensure `mediaController` is fully prepared and has a valid session before attempting to play.
- **Side Panel Settings**:
    - Remove the "Settings" item from the `NavigationDrawerItem` list.
    - Ensure the drawer correctly handles the `isSettingsInDrawer` state.
- **Beautiful Email**: Apply `FontFamily.Cursive` to the email display in the navigation drawer.

#### [MODIFY] [PlaybackService.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/service/PlaybackService.kt)
- **Auth Token Refresh**: Refactor token injection to handle expired tokens by attempting a refresh during data source creation.

### Cloud Integration

#### [MODIFY] [CloudStorageManager.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/data/remote/CloudStorageManager.kt)
- **Thumbnails**: Add `thumbnailLink` and `hasThumbnail` to the fields requested in `listAudioFiles`.
- **Sync Reliability**: Add a method to ensure the "MusicOn" app folder exists before attempting to list or upload files.

#### [MODIFY] [SettingsScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/SettingsScreen.kt)
- **Cleanup**: Remove the "Sync Gdrive" manual sync button.

### User Interface

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Remove Header Timer**: Delete the timer and controls from the `Row` in the player header.
- **Add Integrated Timer Controls**:
    - Add a new row below the Seek Bar in `PlayerControls` containing the Sleep Timer countdown, a Pause/Resume icon, and a Reset icon.
    - Ensure colors match the dynamic `primary` theme color.
- **Visibility**: Apply `LocalIsBackgroundBright` checks to ensure text remains readable against animated backgrounds.

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Bolder Progress Bar**: Stretch the progress bar even further and increase its thickness to be "big" and bold as requested.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Settings**: confirmed it only opens from the gear icon and is gone from the drawer.
- **Playback**: verify cloud and local songs play without error.
- **Timer**: pause and reset the timer from the Player screen controls.
- **Cloud Visuals**: verify song thumbnails appear in the Cloud Browser.
- **Visibility**: confirm text is clear in all animation modes and Day/Night phases.
