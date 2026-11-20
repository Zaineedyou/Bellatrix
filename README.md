# Bellatrix

Bellatrix is a desktop-style Android launcher built entirely in Kotlin. It combines a floating-window workspace, a taskbar, an action center, and a set of built-in utilities in one coherent experience.

## Features

- **Desktop workspace** — floating, movable windows with title bars and task switching
- **Taskbar & action center** — pinned apps, quick controls for Wi‑Fi, mobile data, volume and battery
- **App drawer** — searchable and sortable app list with folders and custom icons
- **Themes & widgets** — configurable colors, wallpapers, icon packs and clock widgets
- **Built-in apps** — file explorer, browser, notepad, music, video, photos and installer
- **Gestures** — configurable swipe-up and swipe-down panels, plus a keybar for terminal input
- **Crash diagnostics** — on any uncaught error, the full log is copied to the clipboard and saved locally

## Requirements

| Component | Version |
|---|---|
| JDK | 17 or newer |
| Android SDK Platform | 36 |
| Android Build Tools | 36.0.0 |
| Gradle wrapper | 8.11.1 |
| Kotlin | 2.3.20 |
| Android Gradle Plugin | 8.9.2 |

Minimum SDK is **23**, target SDK is **36**.

Third-party libraries (AndroidX, Media3, Glide, Gson, AdMob and others) are consumed as Gradle dependencies; no vendored library sources are kept in the project.

## Build

1. Create `local.properties` in the project root and point it at your Android SDK:

   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

   This file is intentionally not committed.

2. Build the release APK:

   ```bash
   ./gradlew clean :app:assembleRelease
   ```

3. The APK is written to:

   ```text
   app/build/outputs/apk/release/app-release.apk
   ```

### Release signing

No signing key is included in this repository. To produce a release APK with a
stable signature, add the following to `local.properties`:

```properties
signing.keystore=/path/to/your/keystore.jks
signing.storePassword=...
signing.keyAlias=...
signing.keyPassword=...
```

The same values can be supplied through the environment variables
`ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and
`ANDROID_KEY_PASSWORD`. When neither is configured, release builds fall back to
the standard Android debug keystore, so local and CI builds still succeed.

## GitHub Actions

`.github/workflows/build-release.yml` builds a release APK on pushes to
`main`/`master`, on `v*` tags, and via manual dispatch. The APK is uploaded as a
workflow artifact, and pushing a `v*` tag also publishes a GitHub Release.

For a stable signature in CI, add these repository secrets:

| Secret | Description |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded JKS keystore (`base64 -w0 keystore.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

If the secrets are absent, the workflow generates a temporary keystore for that
run. Such APKs are installable but use a different certificate on every build,
so existing installs must be removed before updating.

## Install

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Application ID: `com.bellatrixos`.

If an existing installation uses a different signing certificate, remove it first:

```bash
adb uninstall com.bellatrixos
```

## Crash logs

When the app throws an uncaught exception, Bellatrix automatically:

1. Builds a report containing device information, the full stack trace and recent logcat output.
2. Copies that report to the clipboard, so it can be pasted anywhere immediately.
3. Saves a backup to `Download/Bellatrix-crash.txt` (or the app's private files directory if external storage is unavailable).
4. Continues with the normal system crash flow.

## Project layout

```text
app/src/main/kotlin/com/bellatrixos/   Application sources (Kotlin)
app/src/main/res/                      Resources
app/src/main/assets/                   Shaders and bundled assets
app/src/main/AndroidManifest.xml       Manifest
app/build.gradle.kts                   Build configuration and dependencies
tools/                                 Legacy APK signing utility (no keys stored here)
.github/workflows/                     GitHub Actions build workflow
```

## Troubleshooting

- **`SDK location not found`** — create `local.properties` as shown above.
- **`aapt2` fails on ARM hosts** — run the build on an x86_64 machine with the Android SDK installed.
- **Install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`** — uninstall the previous build before installing.
