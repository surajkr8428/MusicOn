# Implementation Plan - Action Hero Figures & Player UI Refactor

This plan addresses the requirement for unique action-figure animations for superheroes, and the refinement of the MiniPlayer docking UI.

## User Review Required

> [!IMPORTANT]
> **Action Figure Animations**: I will implement dynamic sprite animations for each superhero (Superman flying, Spiderman crawling, etc.) using `drawPath` and procedural shapes to represent action figures moving across the screen.
> **Themed Backdrops**: Each mode will have its iconic symbol as a subtle background backdrop.
> **Player UI Integration**: I will integrate the background animation selection directly into the Player screen's "More" menu for instant theme switching.

## Proposed Changes

### 1. Advanced Superhero Animation Overhaul

#### [MODIFY] [StellarBackground.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/StellarBackground.kt)
- **Action Figure Logic**:
    - **BATMAN**: Implement black "Bat-Symbol Rain" drifting down from the top.
    - **SPIDERMAN**: Large spider backdrop. Multiple crawling spider silhouettes moving along web lines.
    - **SUPERMAN**: Pulsing 'S' shield backdrop. Solid hero silhouettes flying across the screen.
    - **IRONMAN**: Multi-layered pulsing Arc Reactors (various movie models) scattered around.
    - **CAPTAIN AMERICA**: Rotating shield rings with multiple running figure silhouettes at the bottom.
    - **BLACK PANTHER**: Heroic pouncing silhouettes fading in and out with purple kinetic pulses.
- **Improved Contrast**: ensuring icons/text turn **White** when any hero mode or Aurora is active.

### 2. Player UI Background Selector

#### [MODIFY] [PlayerScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/PlayerScreen.kt)
- **Menu Integration**: Add "Change Background" to the DropdownMenu.
- **Grid Dialog**: Trigger the reusable `BackgroundGridDialog` for immediate switching.

### 3. Zero-Gap MiniPlayer Docking

#### [MODIFY] [MiniPlayer.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/components/MiniPlayer.kt)
- **Seamless Merging**:
    - Remove vertical/horizontal padding when a sidebar is open.
    - Set the corner radius to 0dp on the side that touches the sidebar.
    - Align left/right precisely against the drawer edge.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Animations**: verify unique action-figure animations for each hero mode.
- **MiniPlayer Docking**: open sidebars and confirm the player band snaps with zero gap.
- **Player UI**: verify background updates instantly via the "More" menu.
- **Visibility**: confirm icons remain readable in all modes.
