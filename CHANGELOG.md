# Changelog

All notable changes to Bellatrix are documented here. Every GitHub Release body is generated
from the matching section of this file by GitHub Actions, so a `v*` tag without a section fails
the release build instead of shipping notes-free update.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Bellatrix uses
`MAJOR.MINOR.PATCH` tags and publishes signed APKs only from GitHub Actions.

## [Unreleased]

## [2.9.25] - 2026-11-22

### Fixed

- Declarative `widget` and `face` files now appear as tiles on the native `MainActivity` home grid, at the real grid cells and size, instead of only being compiled into the unused Compose launcher.
- A tile's own `onTap` and a long-press to edit its `.owl` file work on those native tiles; taps elsewhere still reach Bellatrix's icons, windows and taskbar.
- The taskbar OWL overlay is explicitly transparent and non-focusable, so it no longer leaves a stray circle or press spot on the native taskbar.

## [2.9.24] - 2026-11-22

### Added

- `home.owl` now skins the real Bellatrix desktop area and `taskbar.owl` now skins the real native taskbar, at the size of those views. They are drawn over the existing launcher views, never as a second home screen or a separate runtime.
- Long-press empty home now offers **new widget file**, **edit home surface** and **edit taskbar surface** on the native home used by MainActivity, all opening Bellatrix's own editor.

### Changed

- The seeded `home.owl` and `taskbar.owl` use a full-size `Box`/`Anchor` with hidden `Flex` modules and `anchor` (owl 24). They start transparent so the shipped Bellatrix look stays; set a `background` or add nodes to cover or decorate the native surface.
- The unused Compose prompt bar no longer reserves fixed 40dp/28dp extra strips for OWL; `taskbar.owl` draws over the bar's own bounds instead.
- Agent prompts and bundled widget docs describe the native-surface behavior and the exact edit entries.

### Fixed

- OWL surface files are now drawn from a panel's compiled nodes (the `screen` tree) rather than the empty tile `root`, so editing them changes what is visibly drawn.
- `owl-check` previews `home.owl` at the desktop area's size and `taskbar.owl` at the taskbar's size, without a swipe panel's backdrop, so the PNG matches what OWL draws on the native surface.

## [2.9.23] - 2026-11-22

### Fixed

- Widget removal on the one home page now uses the original compiled spec, so removing a tile no longer leaves a ghost until the next reload.
- Face files and old owl files with a `page:` no longer sort by that stale page number; every file is normalized to page 1 before placement and instance startup.


## [2.9.22] - 2026-11-22

### Added

- Bellatrix home is now hard-coded to exactly one native page. Widgets, faces and pinned apps all land there; there is no empty feed page, no face page, no second tile page and no two-up page on a foldable.
- `home.owl`, `taskbar.owl`, a new `widget-*.owl` and a new `face-*.owl` now ship with working `Flex`, `Box`, `Anchor` and `anchor` examples, so the declarative layouts can be tested immediately without waiting for an agent or a separate edit step.
- When the one home page is full, a new tile takes the least crowded spot and is drawn on top, so a widget created by the user or an agent is still visible on home.

### Changed

- `page:` in any owl file is ignored; `order:` and `size:` still place a new tile, and `~/.config/bellatrix/home/layout.toml` owns its cell afterwards.
- `home.empty-page-left`, `home.empty-page-right` and `home.home-page` are deprecated and removed from the Style menu; old `style.toml` files are migrated to the single-page behavior.
- Face files are now ordinary tiles on the one home page instead of full-screen swipe pages. Widget docs, agent prompts and templates were updated to say so.


## [2.9.21] - 2026-11-21

### Added

- Bellatrix home and taskbar can now be riced in place with OWL. `home.owl` and `taskbar.owl` are declarative `panel` files rendered inside Bellatrix's own bar with `Flex`, `Box`, `Anchor` and the `anchor` property (owl 24), Waybar-style — not a second launcher screen.
- Long-press empty home now offers **new widget file**, **edit home bar** and **edit taskbar**. A widget tile's **edit file** action edits that widget, and empty-page invites create widget or face files directly. All open Bellatrix's native editor without Linux or a coding agent.
- Agent prompts and bundled widget docs now name the exact home/taskbar OWL files and the Flex/Box/Anchor workflow.

### Changed

- `home.owl` and `taskbar.owl` are excluded from gesture panels so they can't be swiped open as a separate screen.
- Bundled widget examples and docs use Bellatrix theme and state paths instead of the previous upstream names.


## [2.9.20] - 2026-11-21

### Fixed

- PC Update dialog no longer crashes on Android 12+ vendors whose `DecorView` is null before the first frame; background blur is applied after `show()` and guarded.
- Guest foot theme now uses the correct color section (`[colors-dark]` on foot 1.26+) and the correct `regularN`/`brightN` keys without a `#` prefix, so terminals start without `invalid section name: colors`.

### Changed

- Launcher update action is now **Download From Github**; the launcher no longer downloads or installs APKs itself.
- Release notes no longer include **Notes** or **Commits since** sections.
- Removed the **Recommended** section from the App Drawer.
- Removed the "Production-ready desktop-style launcher for Android." line from README.


## [2.9.19] - 2026-11-21

### Added

- OWL 24: `Flex` (Waybar-style wrapping flex row), `Anchor` and the `anchor` property for pinning children to an `Anchor`/`Box` edge.
- Bellatrix home and taskbar are agent-editable: taskbar pins live in `~/.config/bellatrix/taskbar/layout.toml`, with `errors.txt` for ignored edits; widgets use the existing `~/.config/bellatrix/widgets/` and home `layout.toml`.
- Coding Agents can create and edit Bellatrix themes in `~/.config/bellatrix/themes/`; local theme folders and backgrounds apply within a second.
- PC Update popup now uses native Bellatrix Liquid Glass (translucent fill, sheen, rim, and background blur on Android 12+).

### Changed

- Removed Omarchy theming completely: no upstream theme release, theme catalog, repo installer, Omarchy paths, shims, or UI strings. Themes are Bellatrix's own local files, using the shipped default palette.
- Terminal keyboard toggle icon and click logic removed; tapping the terminal or a Coding Agents chat box still raises the keyboard.


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
