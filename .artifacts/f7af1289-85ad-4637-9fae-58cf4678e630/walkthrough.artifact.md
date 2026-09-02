# Walkthrough - Header Space Optimization and Player UI Restoration

I have refined the header layout to respect status bar spacing, restored the original "perfect" alignment of the Player UI, and updated the player's minimize icon.

## Changes Made

### Player UI Restoration
- **Original Alignment**: Reverted all recent layout tweaks in `PlayerScreen.kt` (such as image scaling and bottom alignment) to restore the UI to its original state.
- **Minimize Icon**: Replaced the back arrow (`ArrowBack`) with a down angle arrow (`KeyboardArrowDown`) to better represent "minimizing" the player.
- **Header Height**: Restored the player header height to a consistent **64.dp**.
- **System Padding**: Added `statusBarsPadding()` to the player content to ensure icons remain visible and correctly aligned below the status bar.

### Library Header Refinement
- **Restored Top Space**: Reverted the removal of window insets at the top of the app. This ensures the status bar area (the space "above" the MusicOn name) is correctly respected.
- **Minimized Bottom Space**: Used `IntrinsicSize.Min` for the `TopAppBar` in landscape mode across all screens. This removes the excessive vertical gap *below* the title.

### App Sharing Fix
- **Reliable Source Path**: Switched to `context.applicationInfo.sourceDir` to locate the app's APK.
- **Cache-Based Sharing**: The APK is now copied to a dedicated `shared_apk` sub-folder in `context.cacheDir` before sharing.
- **Improved Intent**: Refined the sharing intent with explicit URI permission flags.

## Verification Results

### Automated Tests
- Ran `gradle app:assembleDebug` - **Passed**. The project builds without errors.

### Manual Verification
- Verified the Player UI has its original look and feel.
- Verified the new "Down Arrow" icon for minimizing the player.
- Verified that status bar space is preserved while bottom header gaps are minimized in landscape.
