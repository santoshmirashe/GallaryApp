# 🖼️ GalleryApp — Modern Android Media Gallery

A sleek, modern Android Gallery and Media Viewer built with Kotlin, Jetpack Compose, Material 3, Coil, and Media3 ExoPlayer.

---

## ✨ Features

- **Media Grid**: Fast, smooth scrolling photo and video grid with date grouping.
- **Albums & Folders**: Automatic organization into local device folders and albums.
- **Favorites Collection**: Quick bookmarking of favorite media persisted via Jetpack DataStore.
- **Full-Screen Media Viewer**:
  - High-resolution photo zooming and panning.
  - Video playback powered by AndroidX Media3 ExoPlayer.
- **Modern Jetpack Compose UI**: Clean Material 3 design, edge-to-edge support, and responsive layouts.
- **Permission Flow**: Guided media permissions handling for modern Android versions (Android 13+ granular media permissions).

---

## 🛠️ Technology Stack

- **Language**: Kotlin 2.0.20
- **UI Framework**: Jetpack Compose + Material 3
- **Image Loading**: Coil Compose & Coil Video
- **Video Player**: AndroidX Media3 ExoPlayer
- **Persistence**: Jetpack DataStore Preferences
- **Architecture**: MVVM with Repository pattern, Kotlin Coroutines, and Flow
- **Navigation**: Jetpack Navigation Compose

---

## 🚀 Building & Running

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Signed Release APK
```bash
./gradlew assembleRelease
```
The release APK is generated at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 📄 License
This project is open source and available under the MIT License.
