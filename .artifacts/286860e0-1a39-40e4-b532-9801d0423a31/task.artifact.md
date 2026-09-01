# Task List - Final Pro Features & Stability Fix

## Phase 27: Audio FX Engine (Equalizer & Bass Boost)
- [x] Implement `AudioEffectsController` logic to manage Equalizer, BassBoost, and Virtualizer. [x]
- [x] Create `EqualizerScreen` UI with 5-band sliders and preset dropdown. [x]
- [x] Connect `EqualizerScreen` to `MainViewModel` and `PlaybackService`. [x]

## Phase 28: Smart Playlists & Stats
- [x] Update `TrackEntity` and Room DB to store `playCount` and `lastPlayedTimestamp`. [x]
- [x] Implement automatic stats updating in `MainViewModel` upon song completion. [x]
- [x] Create dynamic tabs/sections for "Most Played" and "Recently Played". [x]

## Phase 29: Sleep Timer & Shake to Change
- [x] Implement `SleepTimer` logic with a countdown notification. [x]
- [x] Create `ShakeDetector` using `SensorManager`. [x]
- [x] Add toggles in Settings for these features. [x]

## Phase 30: Home Screen Widget
- [x] Implement `MusicWidgetProvider` using `RemoteViews`. [x]
- [x] Register Widget in `AndroidManifest.xml`. [x]
- [x] Link Widget buttons to `PlaybackService` actions. [x]

## Phase 31: MP3 Cutter Utility
- [x] Implement `Mp3Cutter` logic using `MediaExtractor` and `MediaMuxer`. [x]
- [x] Create `Mp3CutterScreen` with range sliders and basic waveform. [x]

## Phase 32: Custom Backgrounds & Stability
- [x] Add "Pick Background" feature in Settings using `PickVisualMedia`. [x]
- [x] Update `StellarBackground` to support custom image overlays. [x]
- [x] **CRITICAL FIX**: Fixed app crash by incrementing Room DB version to 3. [x]
- [x] Integrated `accentColor` picker into the global theme. [x]
