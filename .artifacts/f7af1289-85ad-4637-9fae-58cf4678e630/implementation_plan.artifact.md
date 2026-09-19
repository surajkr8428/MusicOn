# Implementation Plan - Nirvaana UX Mastery & Storage Insights

This plan focuses on high-fidelity branding, granular storage insights in the sidebar, a professional "Pop-up" metadata experience for the Grid view, and a robust search experience.

## User Review Required

> [!IMPORTANT]
> **Storage Dashboard**: I am adding a live storage dashboard to your sidebar.
> - **Phone Icon**: Displays the count of local songs.
> - **Cloud Icon**: Displays the count of synced/cloud songs, with a **Sync button** next to it.
> - **Sidebar Behavior**: The sidebar will **no longer close automatically** when you click sync.
> **Grid Info Mastery**: In Grid View, clicking the "i" icon will now show a clean, rectangular technical pop-up instead of expanding the card.
> **Search Precision**: The search bar placeholder will be updated to "Search title, artist, or album", and the logic will be verified to ensure instant matches across all three fields.
> **Logo 3.0**: Redesigning the logo to a premium "N-Headphone" silhouette with no background colors.

## Proposed Changes

### 1. Brand Identity (Nirvaana 3.0)

#### [MODIFY] [ic_nirvaana_logo.xml](file:///D:/MusicOn/app/src/main/res/drawable/ic_nirvaana_logo.xml)
- Redesign with an aesthetic headphone silhouette and a modern 'N' between the earcups.

### 2. Sidebar Storage Dashboard

#### [MODIFY] [MainActivity.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/MainActivity.kt)
- **Dashboard Section**: Remove the sync button from the title row.
- **Local Row**: Phone Icon + Local Song Count.
- **Cloud Row**: Cloud Icon + Cloud Song Count + **Sync Button**.
- **Interaction**: remove `leftDrawerState.close()` from the sync button logic.

#### [MODIFY] [MainViewModel.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/viewmodel/MainViewModel.kt)
- **Cloud Count**: Implement a state flow `cloudTracksCount` to track songs available in Google Drive.
- **Optimized Sync**: Refine `syncAllLocalToCloud` to skip any track with a non-null `gDriveId`.

### 3. Grid View Pop-up Info

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **StellarGridItem**: Update the info icon logic to trigger a `Dialog` (Rectangular Pop-up).
- **Metadata Sheet**: The pop-up will display structured technical specification.

### 4. Search & UI Polish

#### [MODIFY] [LibraryScreen.kt](file:///D:/MusicOn/app/src/main/java/com/example/musicon/ui/screens/LibraryScreen.kt)
- **Search Placeholder**: Update `LibraryTopBar` search field placeholder to "Search title, artist, or album...".
- **Playlist Icons**: Expand the curated set of aesthetic music icons.

## Verification Plan

### Automated Tests
- Build verification: `gradle app:assembleDebug`.

### Manual Verification
- **Search Check**: Type an album name in the search bar and verify matching songs appear instantly.
- **Drawer Stats**: Open the sidebar and verify "Local" and "Cloud" counts are accurate.
- **Grid Info**: Click "i" in Grid view and verify the rectangular pop-up appears.
- **Smart Sync**: Confirm that clicking Sync only triggers uploads for unsynced songs.
- **Logo**: check the new launcher and header logo design.
