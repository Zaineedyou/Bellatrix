# Changelog

All notable changes to Bellatrix are documented here. Every GitHub Release body is generated
from the matching section of this file by GitHub Actions, so a `v*` tag without a section fails
the release build instead of shipping notes-free update.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Bellatrix uses
`MAJOR.MINOR.PATCH` tags and publishes signed APKs only from GitHub Actions.

## [Unreleased]

## [2.9.21] - 2026-11-21

### Added

- Bellatrix home and taskbar can now be riced in place with OWL. `home.owl` and `taskbar.owl` are declarative `panel` files rendered inside Bellatrix's own bar with `Flex`, `Box`, `Anchor` and the `anchor` property (owl 24), Waybar-style — not a second launcher screen.
- Long-press empty home now offers **new widget file**, **edit home bar** and **edit taskbar**. A widget tile's **edit file** action edits that widget, and empty-page invites create widget or face files directly. All open Bellatrix's native editor without Linux or a coding agent.
- Agent prompts and bundled widget docs now name the exact home/taskbar OWL files and the Flex/Box/Anchor workflow.

### Changed

- `home.owl` and `taskbar.owl` are excluded from gesture panels so they can't be swiped open as a separate screen.
- Bundled widget examples and docs use Bellatrix theme and state paths instead of the previous upstream names.

### Important

- DAN PERINTAH ITU (UNTUK TIDAK MEMAKAI TAMPILAN OMALAUNCHER) WAJIB UNTUK UPDATE SETERUSNYA, BUKAN UPDATE INI AJA.
- TOLOL, KAN UDAH GUA BILANG KALO MERGE FITUR OMALAUNCHER KE BELLATRIX TU TAMPILAN NYA SYNC SAMA TAMPILAN BELLATRIX NGENTOT, JANGAN MAKE TAMPILAN OMA LAUNCHER, LU TOLOL APA GIMANA? PINDAH CODING AGENTS NYA STANDALONE JANGAN ADA DI DALAM SETTING LINUX, DAN BUAT SCREEN NYA NYATU KE TAMPILAN SETTING BELLATRIX NYA JANGAN BUAT TERPISAH NGENTOT, INI WAJIB, ANJING

## [2.9.20] - 2026-11-21

### Fixed

- PC Update dialog no longer crashes on Android 12+ vendors whose `DecorView` is null before the first frame; background blur is applied after `show()` and guarded.
- Guest foot theme now uses the correct color section (`[colors-dark]` on foot 1.26+) and the correct `regularN`/`brightN` keys without a `#` prefix, so terminals start without `invalid section name: colors`.

### Changed

- Launcher update action is now **Download From Github**; the launcher no longer downloads or installs APKs itself.
- Release notes no longer include **Notes** or **Commits since** sections.
- Removed the **Recommended** section from the App Drawer.
- Removed the "Production-ready desktop-style launcher for Android." line from README.

### Important

- DAN PERINTAH ITU (UNTUK TIDAK MEMAKAI TAMPILAN OMALAUNCHER) WAJIB UNTUK UPDATE SETERUSNYA, BUKAN UPDATE INI AJA.

## [2.9.19] - 2026-11-21

### Added

- OWL 24: `Flex` (Waybar-style wrapping flex row), `Anchor` and the `anchor` property for pinning children to an `Anchor`/`Box` edge.
- Bellatrix home and taskbar are agent-editable: taskbar pins live in `~/.config/bellatrix/taskbar/layout.toml`, with `errors.txt` for ignored edits; widgets use the existing `~/.config/bellatrix/widgets/` and home `layout.toml`.
- Coding Agents can create and edit Bellatrix themes in `~/.config/bellatrix/themes/`; local theme folders and backgrounds apply within a second.
- PC Update popup now uses native Bellatrix Liquid Glass (translucent fill, sheen, rim, and background blur on Android 12+).

### Changed

- Removed Omarchy theming completely: no upstream theme release, theme catalog, repo installer, Omarchy paths, shims, or UI strings. Themes are Bellatrix's own local files, using the shipped default palette.
- Terminal keyboard toggle icon and click logic removed; tapping the terminal or a Coding Agents chat box still raises the keyboard.

### Important

- DAN PERINTAH ITU (UNTUK TIDAK MEMAKAI TAMPILAN OMALAUNCHER) WAJIB UNTUK UPDATE SETERUSNYA, BUKAN UPDATE INI AJA.

## [2.9.18] - 2026-11-21

### Added

- PC Update popup with the GitHub release version, changelog, and links to the release.
- Direct APK install from the launcher, plus an **Open in Chrome** fallback to the release page.
- `REQUEST_INSTALL_PACKAGES` permission and an update cache path for the launcher FileProvider.
- Changelog-driven GitHub Release notes: What's New, install instructions, SHA-256, commit list, and full changelog link.

### Changed

- Settings navigation order is now **System → Linux → Coding Agents → ...**.
- Coding-agent action label is now **Open** instead of `interactive`.
- PC Update shows the version from `BuildConfig.VERSION_NAME`, so it always matches the APK.
- Release workflow checks out full tag history and requires a matching changelog section before publishing.

### Fixed

- Taskbar Terminal closes when its already-open window is tapped again.
- Every window X now follows the real close path, restores focus to another visible window, and clears the start-menu overlay.
- Coding Agents navigation icon is 20dp like the other settings items.

## [2.9.17] - 2026-11-21

### Fixed

- True close for Settings, Explorer, Linux, agent, and other Bellatrix windows.
- Focus and taskbar state after closing or minimizing a floating window.
- Oversized Coding Agents settings navigation button.

## [2.9.16] - 2026-11-21

### Added

- Per-agent dropdown actions: Open, install, remove, and log.

### Fixed

- Removed the redundant install-all and default-agent settings cards.
- Floating-window focus and full close behavior for Linux and agent windows.

## [2.9.15] - 2026-11-21

### Changed

- Coding-agent launches are generic and proot-safe, with per-agent startup flags and taskbar tags.
- Terminal windows are reused from the taskbar instead of creating duplicates.

## [2.9.14] - 2026-11-21

### Fixed

- Restored one settings card per coding agent after the combined-card regression.

## [2.9.13] - 2026-11-21

### Changed

- Combined coding-agent install card experiment.

## [2.9.12] - 2026-11-21

### Added

- Native Bellatrix Settings page for Coding Agents.
- Bellatrix-only settings cards and dropdowns for agent management.
