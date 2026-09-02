# Implementation Plan - Precision Landscape UI & Robust Sharing

I will fix the orientation transition flicker, eliminate the large gaps in the landscape header, and implement a definitive fix for APK sharing to ensure it installs successfully.

## User Review Required

> [!IMPORTANT]
> - **Zero-Flicker Rotation**: I am fixing the startup logic so the entrance animation only plays once. This will stop the "white screen" effect when rotating the device.
> - **Maximized Landscape Space**: I am removing the excessive padding above and below the "MusicOn" title in landscape mode. The header will be ultra-compact to leave more room for your music.
> - **APK Sharing Fix**: I am switching the sharing method to use a more compatible `ClipData` structure and ensuring the recipient device gets full read access to the installer file.

## Proposed Changes

### UI & Performance Refinements
#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Animation Fix**: Use `rememberSaveable` to ensure `showApp` state persists across rotations. This prevents the animation (and the white screen) from re-triggering when you turn your phone.
- **Header Geometry**: Update the `MusicOnApp` and its children to use zero insets where appropriate, allowing content to sit higher.
- **Robust Sharing**:
    - Copy the APK to the `cacheDir`.
    - Explicitly set `Intent.setDataAndType` and `ClipData` on the sharing intent. This helps some installers that require the data to be in the "Data" field of the intent, not just the "Extra" field.

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Compact Header**:
    - Reduce `LibraryTopBar` height to **40dp** in landscape.
    - Set `windowInsets = WindowInsets(0, 0, 0, 0)` for the `TopAppBar`.
    - Reduce internal vertical padding of the title `Row`.
- **Spacing Cleanup**: Remove redundant `statusBarsPadding()` that was pushing the header down.

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Compact Header**: Reduce header height in landscape.
- **Color Sync**: Ensure Sleep Timer text color is strictly `Color.White`.

## Verification Plan

### Manual Verification
1.  **Rotation Check**: Rotate the device rapidly. Verify there is **no white flicker** and the UI updates instantly.
2.  **Geometry Check**: Verify the "MusicOn" title in landscape is very close to the top edge and the gap below it is minimized.
3.  **Sharing**: Share the APK via Quick Share or WhatsApp. Verify the recipient can install it without "Package invalid" errors.
