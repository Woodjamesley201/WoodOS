# WoodOS 1.0 + WoodDesk

A visually rich, polished launcher and desktop experience for Android tablets — built for and tuned to the **Samsung Galaxy Tab A9 (SM-X110, 8.7-inch)**, and usable on any Android 8.0+ device.

WoodOS is an iOS + One UI inspired translucent home experience. WoodDesk is a Windows 11 inspired desktop environment with real in-app windowing. Both live in one app, and you can switch between them with a smooth transition.

> **Honest scope note:** WoodOS is a normal Android app (launcher APK). It cannot replace the firmware, modify the kernel, or gain system permissions Android does not grant to apps. Every feature below works within those limits — where Android blocks something, WoodOS offers a clearly labeled alternative instead of faking it. See [Android limitations](#android-limitations) for details.

## Features

### WoodOS (Home)
- Translucent, frosted-glass design: Control Center, Notification Shade, search, dock, and folders
- Customizable home screen: clock widget (3 styles), weather widget, app grid with folders, editable dock, search bar
- **Control Center**: quick tiles (Wi-Fi panel, Bluetooth, rotation lock, dark mode, screen record, airplane mode), brightness and volume sliders, battery status
- **Notification Shade**: grouped, expandable notification cards with history and per-app actions (requires notification-listener permission)
- App drawer with All / Recent / Favorites tabs and alphabetical headers
- Edit mode: long-press to rearrange apps, drop apps on the dock, create and edit folders

### WoodDesk (Desktop)
- Windows 11 inspired desktop: centered taskbar, Start menu with pins + recommended + all apps, calendar flyout, system tray
- Real window management for built-in apps: drag, resize, minimize, maximize, close, left/right snap, Alt+Tab window cycling
- Built-in desk apps: File Explorer (SAF + MediaStore), Notes, Calculator, Clock/Calendar, Weather, Task Manager, About, Settings
- Desktop icons with right-click context menus; snap hints while dragging windows
- Keyboard shortcuts: Alt+Tab, Ctrl+Space (Start menu), Escape (close overlays), the Windows key equivalent, Alt+F4

### Customization
- 8 wallpapers (7 presets + dynamic time-of-day) or any image from the gallery
- Light / dark / system themes, 6 accent colors
- Clock styles, widget toggles, home grid columns
- Blur intensity, transparency, and animation-scale sliders — powered by adaptive performance (see below)

### Performance (tuned for the Tab A9)
- Adaptive graphics: blur quality and wallpaper motion automatically step down under memory pressure
- Icon caching (LruCache) and no background processes — WoodOS does nothing while you're not looking at it

## Install

1. Download `WoodOS-debug.apk` (or the signed release APK) from [Releases](../../releases), or build it yourself (below).
2. On the tablet: Settings → Security → allow installation of unknown apps for your browser/file manager, then open the APK.
3. Press Home and choose **WoodOS** as your home screen. Grant the permissions you want:
   - **Notification access** — for the Notification Shade
   - **WRITE_SETTINGS** — for the brightness slider in Control Center
   - **Location (optional)** — for local weather; you can also set a city manually in Settings

## Build from source

Requirements: JDK 17, Android SDK (platform 34).

```bash
git clone https://github.com/Woodjamesley201/WoodOS.git
cd WoodOS
./gradlew assembleDebug        # app/build/outputs/apk/debug/WoodOS-debug.apk
./gradlew test                 # JVM unit tests
./gradlew assembleRelease       # unsigned unless signing env vars are set (see below)
```

### Release signing

The release build falls back to debug signing when no keystore is provided, so the release APK is always installable. To sign properly, set these environment variables (locally or as GitHub Secrets):

```
WOODOS_STORE_FILE=/absolute/path/keystore.jks
WOODOS_STORE_PASSWORD=...
WOODOS_KEY_ALIAS=...
WOODOS_KEY_PASSWORD=...
```

In CI the keystore file is expected base64-encoded in the `WOODOS_KEYSTORE_BASE64` secret.

## CI / Releases

- Every push to `main` compiles the app, runs the unit tests, and uploads `WoodOS-debug.apk` and `WoodOS-release.apk` as build artifacts (`.github/workflows/build.yml`).
- Pushing a tag like `v1.0.0` creates a GitHub Release with both APKs attached (`.github/workflows/release.yml`).

## Android limitations

WoodOS respects Android's security model. Specifically, on Android 13+ (the Tab A9 ships with 13 and is updatable further):

- **Wi-Fi and Bluetooth toggles**: Android 13 removed third-party apps' ability to switch Wi-Fi/BT directly. Control Center opens the system panel for these instead, and clearly labels them as system panels.
- **Freeform windows for external apps**: true desktop-style windowing of other apps requires special system privileges. WoodDesk windows are real for its built-in apps; external apps launch full-screen, with an optional "try freeform" toggle that attempts the unsupported windowing API and falls back gracefully. This is labeled as experimental in Settings.
- **Brightness**: needs the WRITE_SETTINGS app permission, granted via a system dialog the first time.
- **Power off / restart**: not available to apps. The WoodDesk power menu offers what Android allows (and links to the system power options).
- **Screen recording** uses the official MediaProjection API with a system prompt.

## License

MIT — see [LICENSE](LICENSE).
