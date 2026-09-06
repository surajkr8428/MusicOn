# Implementation Plan - Cinematic Superhero Animations & Player UI Integration

This plan focuses on creating high-end, animated superhero backgrounds with iconic symbols and moving figures, and integrating background selection directly into the Player UI.

## User Review Required

> [!IMPORTANT]
> **Cinematic Backgrounds**: I will implement complex procedural and particle-based animations for Batman, Spiderman, Superman, Ironman, and more. This includes "Symbol Rain", "Crawling Spiders", and "Flying Figures".
> **Player UI Integration**: I will add a "Change Background" option to the Player's more menu, allowing for instant theme switching without leaving the music.
> **Rotating Image Fix**: I will resolve the issue where the rotating disc/artwork was hidden or not functioning.

## Proposed Changes

### 1. Advanced Superhero Backgrounds

#### [MODIFY] [StellarBackground.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/StellarBackground.kt)
- **BATMAN**: Implement "Bat-Symbol Rain" similar to the leaf effect but with black bat silhouettes.
- **SPIDERMAN**:
    - Draw a large Spiderman chest symbol as the base background.
    - Implement "Crawling Spiders" using animated particles that move along web lines.
- **SUPERMAN**:
    - Draw a large 'S' shield as the background.
    - Implement "Flying Superman" silhouettes that drift across the screen.
- **IRONMAN**:
    - Randomly generate and pulse various Arc Reactor designs (Mark I, II, triangular, etc.).
- **CAPTAIN AMERICA**:
    - Refine the Shield animation.
    - Add a running Captain America figure silhouette that moves across the bottom/mid section.
- **BLACK PANTHER**:
    - Implement Black Panther silhouettes in various heroic poses that fade in/out and pulse with kinetic energy.

### 2. Player UI Enhancements

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Background Selection**:
    - Add a "Change Background" item to the `DropdownMenu`.
    - Show a selection dialog with the same grid of background modes as the Settings screen.
- **Rotation Fix**: Ensure the `AsyncImage` is correctly clipped and rotated when `PlayerImageMode.ROTATION` is active.

### 3. Settings & Visibility

#### [MODIFY] [SettingsScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/SettingsScreen.kt)
- **Grid Layout**: Ensure the background selection grid is perfectly aligned and displays the mode names clearly.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Batman Mode**: check for black bat symbol rain.
- **Spiderman Mode**: check for crawling spiders and background symbol.
- **Superman Mode**: check for flying figures and background symbol.
- **Ironman Mode**: check for diverse pulsing arc reactors.
- **Player UI**: open player, click "More", select "Change Background", and verify it updates instantly.
- **Rotation**: verify the disc image is back and rotating while playing.
