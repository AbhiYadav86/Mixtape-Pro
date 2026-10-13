# Mixtape Pro

A 90s-themed offline MP3 player for Android (Kotlin + Jetpack Compose), with 50 retro skins.

## Features
| Area | What it does |
|---|---|
| **Player** | Album art behind a live spectrum analyzer. Lyrics automatically replace the spectrum when a song has them (toggle in *VISUAL* menu). |
| **Album art** | Uses embedded ID3 art first. If a song has none, it looks up iTunes (artist-verified), then Deezer. Art is cached on the phone, so it keeps working offline. |
| **Lyrics** | Synced (or plain) lyrics from [LRCLIB](https://lrclib.net), cached per song. Plain lyrics are spread across the song so they still scroll. |
| **Library** | Scans the phone for music on every launch. Keeps favorites, play counts, lyrics and art, and removes deleted files. |
| **Streaming** | 90s internet radio, [Audius](https://audius.co) (trending + search) and the [Internet Archive](https://archive.org). Any track can be saved to the Library or downloaded for offline play (`Music/Mixtape Pro`). |
| **DJ Studio** | **4-Track DJ Mixer**, **Sound Splicer**, **Ringtone Maker** and **Vocal Remover**. All exports are real audio files (WAV or M4A) saved to `Music/Mixtape Pro`, or `Ringtones/Mixtape Pro` for ringtones. |
| **Cloud** | Import from the phone, Google Drive links and web URLs. **Export** sends DJ Studio files (or any track) to Google Drive, Dropbox, email and similar apps through the share sheet, or saves them through the system file picker. |

## Build
- **GitHub (no setup):** every push runs `.github/workflows/build-apk.yml`. Download the APK from the run's **Artifacts** section (`MixtapePro-debug-apk`).
- **Android Studio:** open this folder and run the `app` configuration. Requires JDK 17 or newer and Android SDK 36.

The app id is `com.mixtapepro.player`, so it installs alongside older RetroAmp '99 builds.

## Notes
- Album art, lyrics and streaming need internet the first time. After that, art and lyrics are stored on the phone.
- The vocal remover uses centre-channel cancellation (classic karaoke technique). It works best on stereo studio mixes and cannot separate mono recordings.
