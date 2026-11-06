# Changelog

## 1.0.0 (2026-11-06)

Initial release.

- WoodOS Home: minimalist dark launcher with clock, status chips (Wi-Fi, battery, storage, notifications), favorites with drag-and-drop rearranging and folders, app drawer with search and recents, hidden apps, wallpapers (6 built-in gradients + custom images), accent colors, icon shape/size, light and dark themes.
- WoodDesk: desktop environment with taskbar, start menu (search, favorites, all apps, power options), desktop shortcuts, system tray, floating windows for built-in apps (Notes, Calculator, Files, System info) with drag/resize/minimize/maximize/close, Alt+Tab window switching, keyboard shortcuts, mouse support, external display mirroring via Presentation API, freeform windowing for third-party apps where the device supports it.
- Settings: theme, accent, wallpaper, icons, hidden apps, reset favorites, WoodDesk options, live performance panel, about.
- Performance: no runtime dependencies, ~115 KB release APK, no background services, LruCache icon caching, TIME_TICK-driven clocks.
- CI: GitHub Actions builds debug + release APKs on every push; tags create signed GitHub Releases.
