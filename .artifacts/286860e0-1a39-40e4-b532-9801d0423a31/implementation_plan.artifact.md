# Implementation Plan - Final Layout Precision & APK Fix

I will eliminate the unwanted gap in landscape mode, clean up the player's 3-dot menu, fix the timer color, and use a more robust sharing method for the APK.

## User Review Required

> [!IMPORTANT]
> - **Header Gap Fix**: I am removing `systemBarsPadding()` from the root containers in Library and Player. This will stop the "pushed down" look in landscape mode, making the header sit perfectly at the top.
> - **APK Sharing Fix**: I am switching to `getExternalFilesDir` for the shared APK. This is the most reliable location for Android's installer to read files from when shared via another app.
> - **Player Menu Clean-up**: "Create & Add" and "Change View" have been removed from the 3-dot menu.

## Proposed Changes

### UI Geometry Fixes
#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- Remove `systemBarsPadding()` where it's double-padding.
- Ensure the header uses `statusBarsPadding()` only to sit flush at the top.

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- Change `systemBarsPadding()` to a more controlled inset handling.
- Remove unwanted menu items.
- Set Sleep Timer text color to `Color.White`.

### Sharing Compatibility
#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **New Share Logic**:
    - Use `getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)` to store the temporary `MusicOn.apk`. This directory is publicly readable by installers.
    - Simplified Intent flags.

## Verification Plan

### Manual Verification
1.  **Landscape Check**: Open the app in landscape. Verify the header is at the very top without a large gap.
2.  **3-Dot Menu**: Verify "Create & Add" and "Change View" are gone.
3.  **Timer Color**: Set a timer and verify it matches the white song title.
4.  **APK Share**: Share the APK and install it on another device.
