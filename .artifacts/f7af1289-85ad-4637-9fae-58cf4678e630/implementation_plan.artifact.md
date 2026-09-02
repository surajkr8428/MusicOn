# Implementation Plan - App Force Stop, UI Refinement, and Transition Optimization

This plan addresses the requirement to force stop the app on closure, refines the Player UI in landscape mode, and eliminates the flash/delay during orientation changes.

## User Review Required

> [!IMPORTANT]
> **Force Stop Behavior**: By changing the `onTaskRemoved` behavior to always `stopSelf()`, the music will stop immediately when the user swipes the app away from the recent apps list. This is what was requested, but it differs from many standard music players that keep playing in the background until paused.

> [!NOTE]
> **Smooth Transitions**: I will prevent Activity recreation on orientation changes. This will remove the "white screen" flash and significantly speed up the transition between portrait and landscape modes.

## Proposed Changes

### Media Service & Configuration

#### [MODIFY] [PlaybackService.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/service/PlaybackService.kt)
- Update `onTaskRemoved` to always call `stopSelf()`, ensuring the service (and music) terminates when the app task is removed from recents.

#### [MODIFY] [AndroidManifest.xml](file:///D:/MusicOn/app/src/main/AndroidManifest.xml)
- Add `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout"` to `MainActivity`. This prevents the Activity from being destroyed and recreated during rotation, removing the white screen/delay.

### UI Components & Screens

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Sleep Timer Display**:
    - Update `PlayerLayoutLandscape` and `PlayerControls` to accept `sleepTimerRemaining: Long?`.
    - In `PlayerScreen`, hide the top-level sleep timer message when `isLandscape` is true.
    - In `PlayerControls`, add a bold sleep timer display in the top-right corner of the controls section for landscape mode.
- **Landscape Layout Restructuring**:
    - In `PlayerLayoutLandscape`, wrap the artwork/controls `Row` and the song queue `LazyRow` in a `Column`.
    - Move the song queue `LazyRow` to the bottom of this new `Column`, spanning the full width of the screen.
    - Adjust padding and sizes to ensure the layout matches the provided image.

## Verification Plan

### Automated Tests
- Verify code compiles successfully.
- Ensure all parameters are correctly passed through the composable hierarchy.

### Manual Verification
- **Force Stop**: Open the app, play music, then swipe it away from the recent apps list. The music should stop immediately.
- **Smooth Transition**: Rotate the device. There should be no white flash or significant delay.
- **Landscape UI**:
    - Rotate the device to landscape.
    - Verify the sleep timer appears in the top-right area of the controls with bold text.
    - Verify the song queue icons are at the very bottom of the screen.
