# Building WoodOS

## Requirements

- JDK 17
- Android SDK with platform 35 and build-tools 35.0.0
- Git

The project uses the Gradle wrapper (Gradle 8.9), AGP 8.7.3 and Kotlin 2.0.21, and has **no third-party runtime dependencies**.

## Debug build

```bash
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/WoodOS-debug.apk
```

## Release build (signed)

Release signing is read from environment variables so credentials never enter the repository:

```bash
# One-time: create a keystore
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias woodos -keyalg RSA -keysize 2048 -validity 10000

# Build
WOODOS_KEYSTORE=release.keystore \
WOODOS_STORE_PASSWORD=<your store password> \
WOODOS_KEY_ALIAS=woodos \
WOODOS_KEY_PASSWORD=<your key password> \
./gradlew assembleRelease
# -> app/build/outputs/apk/release/WoodOS-release.apk
```

Without those variables the release APK is built unsigned.

## Tests

```bash
./gradlew testDebugUnitTest
```

Runs the JVM unit tests (calculator engine, favorites serialization).

## CI

GitHub Actions (`.github/workflows/android-build.yml`) builds on every push to `main` and every pull request:

1. Check out the repository
2. Install Java 17 and the Android SDK
3. Run the unit tests
4. Build `WoodOS-debug.apk` and `WoodOS-release.apk`
5. Upload both as build artifacts

Pushing a tag `v*` additionally creates a GitHub Release with both APKs attached. Release signing uses the repository secrets `WOODOS_KEYSTORE` (base64 of the keystore), `WOODOS_STORE_PASSWORD`, `WOODOS_KEY_ALIAS` and `WOODOS_KEY_PASSWORD`; the workflow decodes the keystore at build time and never prints the secrets.

## Setting up release signing secrets

```bash
base64 -w0 release.keystore > keystore.b64
gh secret set WOODOS_KEYSTORE < keystore.b64
gh secret set WOODOS_STORE_PASSWORD
gh secret set WOODOS_KEY_ALIAS
gh secret set WOODOS_KEY_PASSWORD
rm keystore.b64
```

## Project layout

```
app/src/main/java/com/woodos/
  core/           prefs, palette, wallpapers, device info, icon view
  apps/           app repository, favorites model
  home/           WoodOS home screen (launcher)
  desk/           WoodDesk: activity, windows, start menu, built-in apps, presentation
  settings/       WoodOS settings
  notifications/  optional notification listener
  perf/           performance monitor
app/src/test/     JVM unit tests
```
