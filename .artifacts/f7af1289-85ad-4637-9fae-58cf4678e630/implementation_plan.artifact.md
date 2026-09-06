# Implementation Plan - Cinematic Superhero Animations & Dual UI Selectors

This plan focuses on creating high-end, animated superhero backgrounds and integrating background selection directly into BOTH the Settings and Player UI for maximum convenience.

## User Review Required

> [!IMPORTANT]
> **Cinematic Backgrounds**: I will implement complex procedural and particle-based animations for Batman, Spiderman, Superman, Ironman, and more. This includes "Symbol Rain", "Crawling Spiders", and "Flying Figures".
> **Dual Background Selectors**: I will add a "Change Background" option to the Player's more menu AND maintain the grid selector in the Settings screen, allowing for theme switching from anywhere.
> **Rotating Image Fix**: I will resolve the issue where the rotating disc/artwork was hidden or not functioning.

## Proposed Changes

### 1. Advanced Superhero Backgrounds

#### [MODIFY] [StellarBackground.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/StellarBackground.kt)
- **BATMAN**: Implement "Bat-Symbol Rain" with falling black bat silhouettes.
- **SPIDERMAN**:
    - Draw a large Spiderman chest symbol as the base background.
    - Implement "Crawling Spiders" moving along dynamic web lines.
- **SUPERMAN**:
    - Draw a large 'S' shield as the background.
    - Implement drifting "Flying Superman" silhouettes.
- **IRONMAN**:
    - Generate multiple pulsing Arc Reactor designs all around the screen.
- **CAPTAIN AMERICA**:
    - Refine the Shield animation.
    - Add a running Captain America figure silhouette.
- **BLACK PANTHER**:
    - Implement Black Panther silhouettes in various heroic poses that pulse with kinetic energy.

### 2. Dual Selector Integration

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Background Selection Dialog**:
    - Add "Change Background" to the `DropdownMenu`.
    - Create a reusable `BackgroundGridDialog` that can be triggered from both the Player and Settings.
- **Rotation Fix**: Ensure the artwork correctly rotates when `PlayerImageMode.ROTATION` is active.

#### [MODIFY] [SettingsScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/SettingsScreen.kt)
- **Refined Grid**: Ensure the background selection grid is perfectly aligned and intuitive.

### 3. Core Logic

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **State Management**: Ensure background mode changes are applied instantly across the entire app.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Settings Grid**: verify selection works in Settings.
- **Player Selector**: open player menu and verify background updates instantly.
- **Character Modes**: verify unique logos/animations for all superhero modes.
- **Rotation**: verify the disc image is back and rotating while playing.
