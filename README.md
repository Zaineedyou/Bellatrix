# Bellatrix

[![Build Release APK](https://github.com/Zaineedyou/Bellatrix/actions/workflows/build-release.yml/badge.svg)](https://github.com/Zaineedyou/Bellatrix/actions/workflows/build-release.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/Zaineedyou/Bellatrix)](https://github.com/Zaineedyou/Bellatrix/releases/latest)

Bellatrix turns an Android device into a coherent desktop workspace: floating windows, a real
taskbar, a start menu, an action center, and a full suite of built-in utilities. One runtime drives
the whole shell. Guest apps, terminals, files, and coding agents all open inside Bellatrix windows
using Bellatrix UI — there is no second home screen or parallel desktop to switch to.

> [!IMPORTANT]
> Signed release APKs are published **only** by GitHub Actions from `v*` tags. Do not distribute
> locally assembled builds. Every release body is generated from [`CHANGELOG.md`](CHANGELOG.md)
> and includes What's New, install steps, SHA-256, and a full changelog link.

---

## Status

| Area | State |
|---|---|
| Shell | Production — desktop, taskbar, start menu, action center |
| Runtime | Single `App.runtime` for all guest apps and agents |
| UI | Native Bellatrix only |
| Release channel | GitHub Releases only |
| Signing | CI-held keystore via repository secrets |
| License | GPL-3.0 — see [`LICENSE`](LICENSE) |

## Features

### Desktop experience

- Movable and resizable floating windows with title bars, minimize, maximize, and true close
- Taskbar with pinned apps, running-window toggles, and per-window focus state
- Start menu with search, folders, and sortable app list
- Action center with Wi‑Fi, mobile data, volume, brightness, and battery controls
- Gesture-configurable swipe panels and terminal keybar

### Workspace apps

- **File Explorer** — browse, edit, share, and open files in guest apps
- **Browser** — tabs, downloads, finder, and address-bar search
- **Notepad**, **music**, **video**, and **image viewer**
- **Installer** for Android packages
- **Terminal** with one taskbar window and reuse-on-tap behavior

### Settings and customization

- Native Bellatrix Settings with System, Linux, Coding Agents, and PC Update pages
- Coding Agents as a standalone top-level page with one card per agent and per-agent dropdowns
- Themes, wallpapers, colors, icon packs, fonts, taskbar, and home gestures
- OWL widget engine with native schemas and live-data previews
- PC Update with GitHub Release changelog, a **Download From Github** button, and Chrome fallback

### Reliability

- Crash diagnostics copy a full report to the clipboard and save a backup locally
- Release builds are signed, checksummed, and published only from CI
- A `v*` tag without a matching changelog entry fails the release job

### OWL ricing

Bellatrix home is hard-coded to one page. Every widget, face and pinned app lands on that native grid;
an owl file's `page:` is ignored, and a full page places the new tile at the least crowded spot so it
is still visible. Long-press empty home to create a widget file or to edit the two declarative bar files:
`~/.config/bellatrix/widgets/home.owl` and `~/.config/bellatrix/widgets/taskbar.owl`. These seeded files
are ordinary OWL `panel` files rendered inside Bellatrix's own home bar and taskbar — never a second
launcher screen. Lay their modules out with `Flex`, `Box`, `Anchor` and the `anchor` property (owl 24),
Waybar-style; a new `widget-*.owl` or `face-*.owl` uses the same nodes as an ordinary home tile.
A widget tile's **edit file** action and the empty home invite open the same native Bellatrix editor,
so creating and editing OWL files never requires Linux or a coding agent. Agents can still work on the
same files from the standalone Coding Agents settings page.

## Requirements

| Component | Version |
|---|---|
| JDK | 17 or newer |
| Android SDK Platform | 36 |
| Android Build Tools | 36.0.0 |
| NDK | 30.0.16248370 |
| CMake | 3.31.1 |
| Gradle wrapper | 8.11.1 |
| Kotlin | 2.3.20 |
| Android Gradle Plugin | 8.9.2 |

- Minimum SDK: **23** (Android 6.0)
- Target SDK: **36**
- Primary ABI: **arm64-v8a** — bundled guest binaries are arm64 only
- Application ID: `com.bellatrixos`

Third-party libraries (AndroidX, Media3, Glide, Gson, AdMob, and others) are consumed as Gradle
dependencies. Third-party components keep their own license terms.

## Install a release

1. Open the [latest GitHub Release](https://github.com/Zaineedyou/Bellatrix/releases/latest).
2. Download the signed `Bellatrix-<version>-<sha>.apk` asset.
3. Open the APK and accept Android's install prompt. If asked, allow Bellatrix to install unknown apps.
4. You can also open the release page in Chrome and tap the APK directly.

Inside Bellatrix, use **Settings → PC Update → Check for updates** to see the newest release version,
its changelog, and buttons for **Install in launcher** or **Buka di Chrome**.

If replacing a build signed with a different certificate, uninstall the old version first:

```bash
adb uninstall com.bellatrixos
```

## Build from source

Local builds are for development and debugging. **Published releases must come from GitHub Actions.**

1. Create `local.properties` in the project root:

   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

2. Run the checks and a debug build:

   ```bash
   ./gradlew :owl:test :owl:owlJar --no-daemon --stacktrace
   ./gradlew :app:assembleDebug --no-daemon --stacktrace
   ```

3. CI also compiles `patches/proot/fastpath/fastpath.c` into
   `app/src/main/jniLibs/arm64-v8a/libfastpath.so` with the NDK before the APK build.

### Release signing secrets

The release workflow requires all four repository secrets. Missing secrets fail the build instead
of producing an unsigned or differently signed APK.

| Secret | Description |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded JKS keystore (`base64 -w0 keystore.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

The keystore is never committed to this repository.

## Release process

1. Add the version section to [`CHANGELOG.md`](CHANGELOG.md).
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Push `master` and wait for **Build Release APK** to succeed.
4. Push the `v<version>` tag. GitHub Actions then:
   - builds and signs the APK from CI,
   - generates the release body from the changelog section,
   - records SHA-256, version code, commit list, and full changelog link,
   - uploads the APK to the GitHub Release.

A tag without a matching changelog section is rejected. This keeps every release documented.

## Project layout

```text
app/src/main/kotlin/com/bellatrixos/   Application sources (Kotlin)
app/src/main/res/                      Resources
app/src/main/assets/                   Shaders and bundled assets
app/src/main/AndroidManifest.xml       Manifest
app/build.gradle.kts                   Build configuration and dependencies
patches/proot/fastpath/                CI-built fast-path native source
tools/                                 Legacy APK signing utility (no keys stored here)
.github/workflows/                     GitHub Actions build and release workflow
CHANGELOG.md                           Source of truth for release notes
LICENSE                                GNU GPL v3
```

## Troubleshooting

- **`SDK location not found`** — create `local.properties` as shown above.
- **`aapt2` fails on ARM hosts** — build on x86_64 with a full Android SDK, or use GitHub Actions.
- **Install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`** — uninstall the previous build first.
- **Release has empty notes** — add a `## [<version>]` section to `CHANGELOG.md` before tagging.

## Crash reports

On an uncaught exception, Bellatrix builds a report with device details, the full stack trace, and
recent logcat output. It copies the report to the clipboard and saves a backup to
`Download/Bellatrix-crash.txt`, falling back to private app storage when external storage is
unavailable.

## Requirement for every update

DAN PERINTAH ITU (UNTUK TIDAK MEMAKAI TAMPILAN OMALAUNCHER) WAJIB UNTUK UPDATE SETERUSNYA, BUKAN UPDATE INI AJA.

All Bellatrix features, including OWL widgets, the taskbar, themes, and Coding Agents, use the native Bellatrix UI only.

## License

Bellatrix is free software licensed under the **GNU General Public License, version 3**.
See [`LICENSE`](LICENSE) for the full text.

This project is not affiliated with Microsoft and is not Microsoft Windows or Windows 365.
