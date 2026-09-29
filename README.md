# 🎵 MelodiQ — Offline Music Player

MelodiQ is a feature-rich Android **offline music player** designed for local music playback and a smooth, customizable listening experience. It focuses on playing music stored on the user's device while providing modern playback controls, queue management, playlists, audio enhancements, background playback, and persistent playback state.

> **Project status:** Feature-complete offline music player with a few playback-engine refinements/testing areas remaining, particularly true overlapping crossfade and final gapless/ReplayGain validation.

---

## ✨ Features

### 🎵 Local Music & Library

MelodiQ works with music stored locally on the device, allowing users to browse and play their own music collection without requiring a streaming service.

- 🎵 Local music/library scanning
- 🔎 Local music browsing and search
- 📚 Curated queues
- 📝 Lyrics support
- 📂 Playlist support
- ❤️ Favorites

### ▶️ Playback Controls

Core playback functionality is handled through the app's playback engine.

- ▶️ Play / Pause
- ⏭️ Next track
- ⏮️ Previous track
- ⏩ Forward seeking
- ⏪ Backward seeking
- 🎚️ Playback speed control
- 🔀 Shuffle
- 🔁 Repeat modes

### 🔁 Repeat Modes

MelodiQ supports multiple repeat behaviors:

- 🔁 Repeat Off
- 🔂 Repeat One
- 🔁 Repeat All

These modes work with the current playback queue.

### 🔀 Queue Management

The playback queue can be managed dynamically without rebuilding the entire playback system.

- ➕ Add tracks to queue
- ➖ Remove tracks from queue
- ↕️ Reorder queue items
- ⏭️ Skip to another queued track
- 🔀 Shuffle queue
- 📚 Load curated/playlist-based queues

### 🎧 Background Playback

Music can continue playing while the user:

- Uses other apps
- Locks the device
- Leaves the main player screen

The playback architecture uses Android's media playback components to keep playback independent from the main UI.

### 🔔 Media Notification

MelodiQ provides media playback controls through the Android notification/lock-screen media interface.

Users can control playback without reopening the application.

Supported controls include:

- ▶️ Play / Pause
- ⏭️ Next
- ⏮️ Previous
- 🎵 Current track information
- 🎧 Background playback controls

### 📱 MediaSession Integration

The player integrates with Android's media-session system, allowing external playback controls to communicate with the music player.

This helps support:

- 🔔 System media controls
- 🎧 Headset/Bluetooth controls
- 🚗 External media controls
- 🔒 Lock-screen playback controls

### 💾 Playback-State Restoration

MelodiQ persists important playback information so the listening session can be restored.

The application keeps track of information such as:

- 🎵 Current track
- ⏱️ Playback position
- ▶️ Playback state
- 🔁 Repeat state
- 🔀 Shuffle state
- ⚙️ Relevant playback settings

This allows the player to continue from the previous listening state instead of always starting from the beginning.

### ❤️ Favorites

Users can mark tracks as favorites for quick access.

Favorites are persisted locally and can be used as a dedicated collection of preferred music.

### 📂 Playlists

MelodiQ supports local playlists for organizing music into custom collections.

Users can build playlists based on their preferred tracks instead of relying only on the device's folder/library structure.

### 💤 Sleep Timer

The sleep timer allows playback to automatically stop after a configured period.

Useful when listening to music before sleeping without leaving playback running indefinitely.

### 🎛️ Equalizer

MelodiQ includes an audio equalizer for adjusting the listening experience according to user preference.

The equalizer is separate from the core queue/player logic so playback management can continue independently.

### 🔊 Volume Normalization / ReplayGain

MelodiQ includes infrastructure for **ReplayGain-style volume normalization**.

When compatible ReplayGain metadata is available in an audio file, the player can use the embedded loudness information to adjust playback gain and reduce large perceived volume differences between tracks.

Conceptually:

```text
Track A  ──► ReplayGain metadata ──► Gain adjustment
Track B  ──► ReplayGain metadata ──► Gain adjustment
Track C  ──► No metadata ───────────► Normal playback
```

This feature is designed to work as an enhancement rather than a requirement for playback.

### 🌊 Crossfade

MelodiQ provides configurable crossfade settings for transitions between tracks.

The current playback implementation includes fade-out/fade-in transition logic and a configurable duration.

Conceptually:

```text
Track A  ███████████████╲
                         ╲
                          ╲
                           ╲████████████ Track B
```

> **Note:** The current implementation provides sequential fade-out/fade-in behavior. If the final requirement is a true overlapping crossfade where both tracks play simultaneously during the transition, that remains a playback-engine refinement.

### 📚 Curated Queues

MelodiQ supports curated queues for controlling which tracks are played together.

This allows playback to be driven by selected collections rather than only the complete local library.

### ⚙️ Persistent Settings

Important player preferences are persisted locally so users do not need to configure them again every time the app starts.

Examples include:

- 🔊 Volume normalization preference
- 🌊 Crossfade preference/duration
- 🎚️ Playback settings
- 🔁 Repeat state
- 🔀 Shuffle state

---

## 🏗️ Playback Architecture

The playback system is organized around a central playback service/player architecture.

High-level flow:

```text
┌─────────────────────┐
│        UI           │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ ViewModel / Control │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Playback Service    │
│ / MediaSession      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│     ExoPlayer       │
│       Media3        │
└──────────┬──────────┘
           │
     ┌─────┼───────────────┐
     ▼     ▼               ▼
  Queue  ReplayGain    Crossfade
     │     │               │
     └─────┴───────┬───────┘
                   ▼
             Audio Output
```

This architecture keeps queue management, playback state, media controls, and audio enhancements separated from the main UI.

---

## 🧩 Feature Summary

| Feature | Status |
|---|---|
| 🎵 Local music/library scanning | ✅ Implemented |
| ▶️ Play / Pause | ✅ Implemented |
| ⏩ Seek / Forward / Backward | ✅ Implemented |
| 🔀 Queue management | ✅ Implemented |
| 🔁 Repeat modes | ✅ Implemented |
| 🔀 Shuffle | ✅ Implemented |
| 🎚️ Playback speed | ✅ Implemented |
| 🎧 Background playback | ✅ Implemented |
| 🔔 Media notification | ✅ Implemented |
| 📱 MediaSession integration | ✅ Implemented |
| 💾 Playback-state restoration | ✅ Implemented |
| ❤️ Favorites | ✅ Implemented |
| 📂 Playlists | ✅ Implemented |
| 💤 Sleep timer | ✅ Implemented |
| 🎛️ Equalizer | ✅ Implemented |
| 📝 Lyrics | ✅ Implemented |
| 🔊 ReplayGain / volume normalization | ⚠️ Implemented; real-file validation recommended |
| 🌊 Crossfade settings | ✅ Implemented |
| 🌊 True overlapping crossfade | ⚠️ Remaining refinement |
| 📚 Curated queues | ✅ Implemented |
| 🔎 Local browsing/search | ✅ Implemented |
| ⚙️ Persistent playback/settings state | ✅ Implemented |

---

## 🎯 Current Development Status

The core offline music-player functionality is already implemented.

The remaining playback-engine work is primarily refinement and validation:

1. **Gapless playback**
   - Verify real-world transitions using suitable gapless audio.
   - Avoid unnecessary custom implementation if Media3/ExoPlayer already provides the required behavior.

2. **ReplayGain**
   - Test files containing actual ReplayGain metadata.
   - Validate behavior across supported audio formats.

3. **Crossfade**
   - Current fade-out/fade-in logic is implemented.
   - A true overlapping crossfade would require both tracks to overlap during the transition.

These refinements should be made without replacing the existing queue, MediaSession, background playback, or playback-state architecture.

---

## 🛠️ Technology

The project uses Android's modern media playback architecture, including:

- 🤖 Android
- 🎵 Media3 / ExoPlayer
- 📱 MediaSession
- 💾 Local persistence
- 🎧 Background media playback
- 🔔 Android media controls

---

## 📌 Project Goal

MelodiQ is designed to provide a complete local/offline music experience without depending on a streaming backend for core playback.

The goal is to combine:

> **Local music + powerful playback controls + queue management + audio customization + persistent playback state**

into a single Android music-player experience.

---

## 🚀 Roadmap

Potential future improvements include:

- 🎚️ Finalize true overlapping crossfade
- 🎵 Extensive gapless playback testing
- 🔊 Expanded ReplayGain format/tag validation
- 📊 More detailed playback statistics
- 🎨 Additional player customization
- 🧪 Automated playback regression tests
- 📱 Broader device/audio-format compatibility testing

---

## 📄 License

© 2027 Md. Tasnimul Hasan. All rights reserved.

---

Made with ❤️ for offline music playback.
