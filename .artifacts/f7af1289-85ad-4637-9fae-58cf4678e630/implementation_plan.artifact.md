# Implementation Plan - Nirvaana UX Final Tier & Fixes

This plan resolves the reported UI issues, enables player gestures, and ensures app sharing works correctly by refining the logic and fixing layout weights.

## User Review Required

> [!IMPORTANT]
> **Crash Fix (State Stability)**: I've identified that the crash was due to `PagerState` reacting to dynamic list updates. I am stabilizing the `pagerState` and its interaction with `customFolders`.
> **Always Visible Queue**: Redesigning `PlayerLayoutPortrait` to use a `Box` with a fixed-height constraint for the `LazyRow` at the bottom, ensuring it is never pushed off-screen.
> **True Swipe Gestures**: Implementing `Modifier.pointerInput` with `detectHorizontalDragGestures` on the main player artwork to skip tracks (Left -> Next, Right -> Previous).
> **App Sharing (APK)**: Fixing the `FileProvider` logic in `MainViewModel` to ensure the APK is copied to a shared directory with correct permissions before launching the share intent.
> **Immortal Library Polish**: Ensuring `triggerBackup()` is robust and handles settings synchronization properly.

## Proposed Changes

### 1. Stability & Build Polish

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- Fix the `pagerState` initialization.
- Remove redundant semicolons and logic that might trigger `IllegalStateException`.

### 2. Player UI & Gestures

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Swipe Skip**: Add horizontal drag detection to the main artwork box.
- **Queue Visibility**: Refactor the portrait layout to guarantee bottom icons are always rendered in a dedicated row at the bottom of the screen.

### 3. Share The App (APK Fix)

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- Refine `shareAppApk()` to use `java.io.File` and ensure the `shared_apk` folder is properly managed within `externalCacheDir`.

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- Update the sidebar "Share" item to point to the `viewModel.shareAppApk()` method.

### 4. Compact Specs & Percentage Display

#### [MODIFY] [CircularSyncProgressBar.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/CircularSyncProgressBar.kt)
- Ensure the percentage is derived from `current / total` in the `Paused` state.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Stability**: Launch on mobile and rotate; verify no crashes.
- **Gestures**: Swipe album art in player and verify track skips.
- **Sharing**: Click "Share APK" and verify a sharing sheet opens with a valid `.apk` file.
- **Queue**: verify song icons are visible on small devices.
