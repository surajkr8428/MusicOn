# Implementation Plan - Final Stability, Persistence & Advanced UI

This plan addresses the song playback issue, sign-in persistence, crashes, and advanced UI polish including elongated progress bars and enhanced cloud browser visuals.

## User Review Required

> [!IMPORTANT]
> **Sign-in Persistence**: I will add a check in `MainActivity.onCreate` to automatically verify and restore the Google Sign-in state from the last session.
> **Text Visibility**: To fix visibility on bright backgrounds (like Day phase), I will implement a `AdaptiveContentColor` logic that switches between high-contrast light/dark text based on the current background phase.

## Proposed Changes

### Stability & Persistence

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Auto Sign-in**: Check `GoogleSignIn.getLastSignedInAccount` in `onCreate` and update `viewModel.updateSignInStatus(true)` immediately.
- **Fix Playback**: Refine `toMediaItem()` to handle both `content://` and `file://` paths correctly using `Uri.fromFile` for local paths.
- **Crash Prevention**: Add basic try-catch around `mediaController` interactions and ensure `lifecycleOwner` is handled safely.

### UI Refinement

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Stretched Progress Bar**: Update `SyncProgressBar` to use a `Box` with `weight(1f)` and a bolder thickness (10dp). Move the Search icon to the right of the progress bar if needed, or keep it elongated in between.
- **Visibility**: Update all `Text` colors in `LibraryTopBar` and `HeaderStatusPill` to use a context-aware color (Black on bright, White on dark).

#### [MODIFY] [StellarBackground.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/StellarBackground.kt)
- **New Animation Modes**: Ensure "SPACE", "NEBULA", and "AURORA" are fully implemented with distinct Canvas drawing logic.
- **Light Theme Support**: Provide a `CompositionLocal` or shared state for "IsBackgroundBright" to help child components adjust their text color.

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt) - `CloudBrowserScreen`
- **Thumbnails**: Use `file.thumbnailLink` in the `CloudBrowserScreen` items (Grid and List) to show actual song artwork where available.
- **Beautiful Email**: Apply `FontFamily.Cursive` and `FontWeight.Bold` to the email display in the navigation drawer.

### Playback Logic

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **Session Protection**: Ensure `isUserSignedIn` is persisted in `DataStore` or simply rely on `GoogleSignIn.getLastSignedInAccount`.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Sign-in Persistence**: Sign in, close app from recents, reopen, and verify you are still signed in.
- **Playback**: Click a song and verify audio starts.
- **Progress Bar**: Verify it is long, thick, and positioned between title and search.
- **Cloud Thumbnails**: Open Cloud Browser and verify artwork images load.
- **Theme/Visibility**: Check text readability in both Light and Spotify Dark modes.
