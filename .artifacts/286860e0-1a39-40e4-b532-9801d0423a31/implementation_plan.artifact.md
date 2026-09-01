# Implementation Plan - Advanced UI Features & APK Sharing Fix

I will implement a set of high-impact features including a top-level sleep timer, adaptive view modes (List/Grid), professional startup animations, and a fix for the APK sharing system.

## User Review Required

> [!IMPORTANT]
> - **APK Sharing Fix**: To ensure the shared app can be installed easily, I will copy the APK to a temporary file named `MusicOn.apk` before sharing. This gives the recipient a clear filename and helps Android's installer identify it correctly.
> - **View Modes**: You can now switch between **List** and **Grid** views in every tab. The Grid view will show large artwork with the name underneath.
> - **Top Timer**: If a sleep timer is active, the remaining time will be displayed at the top of the library for easy tracking.

## Proposed Changes

### Library UI & Navigation
#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **TopBar Timer**: Add an indicator next to the search icon showing `Remaining: XXm` if the sleep timer is active.
- **View Mode Switcher**: Add a List/Grid toggle in the `LibraryTopBar`.
- **Adaptive Grid**: Implement `StellarGridItem` for the grid view mode.
- **Sorting Logic**: Ensure sorting works seamlessly with both List and Grid views.

### Startup & Aesthetics
#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Startup Animation**: Wrap the `MusicOnApp` in an `AnimatedVisibility` block to create a professional fade-in and subtle scale-up effect when the app first loads.
- **APK Share Fix**:
    - Copy `packageResourcePath` to `cacheDir/MusicOn.apk`.
    - Use `FileProvider` on the new named file.
    - This ensures the recipient sees "MusicOn.apk" instead of "base.apk".

### Data & State
#### [MODIFY] [SettingsRepository.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/data/SettingsRepository.kt)
- Ensure `LIBRARY_VIEW_MODE` is correctly persisted for the new manual switcher.

## Verification Plan

### Manual Verification
1.  **Timer Check**: Set a sleep timer for 15 minutes. Verify "15m" appears at the top of the library.
2.  **View Toggle**: Switch to Grid view. Verify artwork is large and titles are centered below. Switch back to List.
3.  **Share Test**: Tap "Share App" in the sidebar. Verify the sharing dialog shows the file name as "MusicOn.apk".
4.  **Startup**: Kill the app and reopen. Observe the smooth entrance animation.
