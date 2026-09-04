# Implementation Plan - Final UI/UX Refinement & Stability Fixes

This plan focuses on improving text visibility in dynamic backgrounds, fixing the Google Sign-in flow, separating status from progress in the header, and adding requested visual polish.

## User Review Required

> [!IMPORTANT]
> **Sign-in Flow**: I will refactor the sign-in result handling to ensure the UI updates immediately and doesn't crash.
> **Status Separation**: The main header will now have two distinct elements: a small connectivity pill (ONLINE/OFFLINE) and a longer, independent progress bar when syncing.

## Proposed Changes

### Core Fixes & Stability

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Fix Sign-in Result**: Update `signInLauncher` to explicitly call `viewModel.updateSignInStatus(true)` upon success.
- **Auto-check Sign-in**: Ensure the app checks the existing account on startup.
- **Cloud Security Pop-up**: If a user clicks "Cloud Browser" or "Sync" while signed out, show a prompt to sign in.

#### [MODIFY] [MainViewModel.kt](file:///D:/MainViewModel.kt)
- **Status States**: Add explicit states if needed for independent connectivity and sync progress visibility.

### UI Refinement

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Header Status Separation**:
    - Split the unified indicator into a small **Connectivity Pill** (ONLINE/OFFLINE) and a separate **Sync Progress Bar**.
    - **Long & Bold Progress Bar**: The progress bar will be elongated to fill the available space in the header and made thicker (8dp height) for a "bold" professional look.
- **Sleep Timer Styling**: Update the timer text color to use `MaterialTheme.colorScheme.primary` for better visibility and consistency.

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Animated Background**: Ensure `StellarBackground` is the base layer for all player views.
- **Sleep Timer Styling**: Change timer color to `primary`.
- **Text Contrast**: Use adaptive colors (Black/White) based on background luminance if possible, or ensure surfaces have enough opacity.

#### [MODIFY] [StellarBackground.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/StellarBackground.kt)
- **Contrast Improvement**: Provide a way for the theme to know if the background is "Light" or "Dark" based on the current time phase, and adjust text colors accordingly.

#### [MODIFY] [SettingsScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/SettingsScreen.kt)
- **Clean up**: Remove the redundant "Theming" header text above the theme mode buttons.

#### [MODIFY] [Theme.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/theme/Theme.kt)
- **Mode Fix**: Ensure `themeMode` state is correctly respected and triggers a full UI color refresh.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Sign-in**: Verify account picker opens and app updates to "Signed in" state without closing.
- **Header**: Verify independent ONLINE pill and a long progress bar during sync.
- **Player**: Verify animated Day/Night background is visible behind controls.
- **Settings**: Verify "Theming" text is gone and theme switching works.
- **Visibility**: Check text readability during "Day" phase (bright blue background).
