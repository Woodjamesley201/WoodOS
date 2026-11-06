# Installing WoodOS

WoodOS is a normal Android app (a launcher). It works on any Android 8.0+ tablet, needs no root access, and does not modify your system.

## 1. Download the APK

- **Recommended:** get `WoodOS-release.apk` from the latest release on the repository's Releases page. Releases are built and signed automatically by GitHub Actions.
- Alternatively, download `WoodOS-debug.apk` from a build's artifacts (Releases page → any build → Artifacts). The debug build is larger and slightly slower but works the same.

## 2. Install

1. Open the downloaded APK on your tablet.
2. If asked, allow your browser or file manager to "Install unknown apps". (Settings → Security → Install unknown apps, then retry.)
3. Tap **Install**.

If installation fails with "App not installed", check that you are not installing over a different signature (e.g. debug over release) — uninstall the old one first.

## 3. Make WoodOS your default launcher

1. Press the **Home** button.
2. Android shows a chooser — select **WoodOS** and tap **Always**.
3. To change this later: Settings → Apps → Default apps → Home app.

## 4. First steps in WoodOS Home

- The home screen shows the clock, date, status chips, your favorites and the bottom bar (Apps / WoodDesk / Settings).
- Tap **Apps** to open the drawer. Long-press an app for options: add to favorites, create a folder, hide the app, or open its app info.
- Long-press a favorite and drag to rearrange it; drop it on the trash circle that appears to remove it. Dropping one app onto another via "Create folder with…" makes a folder.
- Personalize in **Settings**: theme (dark/light/system), accent color, wallpaper, icon shape and size, hidden apps.

## 5. Activate WoodDesk

- Tap **WoodDesk** in the home bar. That's it — WoodDesk is a separate screen, your launcher stays active underneath.
- In WoodDesk: the **start button** (WoodOS mark, bottom-left) opens the start menu with search, favorites, all apps, settings and power options.
- The taskbar pins Notes, Calculator, Files and System info — these open in real floating windows you can drag, resize, minimize, maximize and close.
- Keyboard shortcuts: **Alt+Tab** switches windows, **Esc** closes the start menu, **Alt+F4** closes the active window, the **Meta/Windows key** opens the start menu. Bluetooth and USB keyboards and mice are supported.
- Other apps open through Android's own windowing: if your tablet supports freeform/desktop windows they appear in desktop-style windows; otherwise they open full-screen (WoodOS will tell you which case applies on your device).

## 6. Optional: notification count

WoodOS can show how many notifications are active. Grant it once:

Settings → Apps → Special access → Notification access → WoodOS Notifications.

This is optional; WoodOS works fully without it.

## Troubleshooting

| Problem | Fix |
|---|---|
| WoodOS doesn't appear in the Home chooser | Settings → Apps → Default apps → Home app → WoodOS |
| The clock or status chips look stale | They refresh on every unlock/resume; pull down the system shade once if a tick was missed |
| Apps open full-screen inside WoodDesk | Your device doesn't support freeform window management — this is an Android platform limitation, not a bug |
| Notes files are gone | Notes are stored in `Android/data/com.woodos/files/notes/` on internal storage; reinstalling the app removes them |
| Something misbehaves | Settings → About shows the version; uninstalling restores your previous launcher completely |
| I want my old launcher back | See below |

## Returning to your original launcher

Nothing is modified system-wide, so switching back is one tap:

1. Settings → Apps → Default apps → Home app.
2. Choose your old launcher (e.g. One UI Home, Pixel Launcher).

To remove WoodOS completely, uninstall it like any other app. Your wallpaper, apps and data are untouched.
