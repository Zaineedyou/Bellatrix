# Changelog

All notable changes to Bellatrix are documented here. Every GitHub Release body is generated
from the matching section of this file by GitHub Actions, so a `v*` tag without a section fails
the release build instead of shipping notes-free update.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Bellatrix uses
`MAJOR.MINOR.PATCH` tags and publishes signed APKs only from GitHub Actions.

## [Unreleased]

## [2.9.19] - 2026-11-21

### Added

- OWL 24: `Flex` (Waybar-style wrapping flex row), `Anchor` and the `anchor` property for pinning children to an `Anchor`/`Box` edge.
- Bellatrix home and taskbar are agent-editable: taskbar pins live in `~/.config/bellatrix/taskbar/layout.toml`, with `errors.txt` for ignored edits; widgets use the existing `~/.config/bellatrix/widgets/` and home `layout.toml`.
- Coding Agents can create and edit Bellatrix themes in `~/.config/bellatrix/themes/`; local theme folders and backgrounds apply within a second.
- PC Update popup now uses native Bellatrix Liquid Glass (translucent fill, sheen, rim, and background blur on Android 12+).

### Changed

- Removed Omarchy theming completely: no upstream theme release, theme catalog, repo installer, Omarchy paths, shims, or UI strings. Themes are Bellatrix's own local files, using the shipped default palette.
- Terminal keyboard toggle icon and click logic removed; tapping the terminal or a Coding Agents chat box still raises the keyboard.

### Notes

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
