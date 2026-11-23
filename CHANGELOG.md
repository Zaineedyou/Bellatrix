# Changelog

All notable changes to Bellatrix are documented here. Every GitHub Release body is generated
from the matching section of this file by GitHub Actions, so a `v*` tag without a section fails
the release build instead of shipping notes-free update.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Bellatrix uses
`MAJOR.MINOR.PATCH` tags and publishes signed APKs only from GitHub Actions.

## [Unreleased]

## [2.9.42] - 2026-11-24

### Added

- Coding agents in Linux can now modify Bellatrix's native views and UI logic from `~/.config/bellatrix/native-ui/`. `look.toml` matches real taskbar, start menu, action center, Settings, window and home views by resource id or class and changes their size, padding, background, border, text and icons in place.
- An embedded Lua runtime inside the existing `App.runtime` runs `ui.lua` from the same directory. Scripts can hook clicks, long-presses and gestures, edit native adapter data, pin taskbar apps, open the existing Bellatrix Settings page and change window decorations. No second Activity, launcher runtime, home, drawer, Settings window or replacement renderer was added.
- Both files are watched while the launcher runs: a save reaches the views already on screen within a second, and the shared `errors.txt` names anything ignored. The shipped files are neutral templates, with active taskbar and Settings examples in `examples/`.
- `README.md` beside those files documents every surface, selector, property and `nativeui` API call, and the guest `AGENTS.md` points agents to it.

## [2.9.41] - 2026-11-23

### Fixed

- A widget's `screen { }` now opens over Bellatrix's native home, above its windows, drawer and taskbar. Back closes that screen instead of leaving the launcher.
- Animated home widgets pause while the native start menu, an app window, the window scrim or an open widget screen covers home, and resume when home is clear again.

## [2.9.40] - 2026-11-23

### Fixed

- Linux terminal windows now use the same Keyboard suggestions setting as home terminals, so word and clipboard suggestions from the phone keyboard work there too.
- Restored the Keyboard suggestions switch in native Settings → Time & Language → Typing after the Compose Settings screens were removed.

## [2.9.39] - 2026-11-23

### Fixed

- Permissions on Android 13 (SDK 33): the native Settings page now registers permission dialogs with the fragment that owns them, so reopening the page no longer crashes with "Can not register for activity result after the activity has been resumed".
- Settings text now starts each sentence with a capital letter and stays in English in every translation. Licenses now uses the exact label "Open Source Software In Bellatrix".

### Removed

- The built-in Compose Settings port and its separate style, Linux, store and gesture screens. Existing shortcuts and widget links now open the matching Bellatrix-native Settings pages, with one runtime, one home and one settings window.

## [2.9.38] - 2026-11-23

### Fixed

- Restored the phone-folder picker helper used by the file manager and fixed the native Permissions fragment's saved-state imports; the Settings port compiles again.

## [2.9.37] - 2026-11-23

### Fixed

- Licenses, Packages and Permissions now open as Bellatrix-native Settings pages built from the launcher's own cards, instead of the ported Compose screens. Existing links to those pages land on the native ones.
- Licenses, package and permission strings name Bellatrix instead of Omalauncher in every translation.

## [2.9.36] - 2026-11-22

### Fixed

- File manager: initialized the copy/move worker executor, corrected inverted delete logic for multi-item copy, propagated folder-copy failures, and made the conflict dialog cancel path release the worker.
- Browser and apps: sanitized JS-download filenames, disabled file-origin cross-origin access, stopped clearing the shared main-handler callbacks, and fixed zoom, popup, clipboard, permission, download-receiver, playback and track-state handling.
- Native launcher: corrected taskbar context-menu targeting, Linux key-bar bridge ownership, editor shim recreation, leave animations, start-menu items, desktop rename indexing, keyboard delete handling, folder geometry and pulled-panel state.
- Settings and Linux: guarded the home-role check on Android 6–9, made package/GPU/CPU state observable while work runs, and keyed store result messages to the selected entry.
- Widgets and OWL: fixed native taskbar skin leaks/state/dp/padding, icon caching, animation specs and preview cleanup; added telephony permission and backoff, audio-capture restart cleanup, AirPods fallback and image retry; corrected `min`/`max`, empty-string `replace`, shader animation detection and the removed `appAction("share")` word.
- Theme, voice and wallpaper: guarded API 28 image decoding, validated theme colors, filtered guest images, made wallpaper teardown asynchronous, fixed Codex stop and Pi retry reporting, released Media3 transformers and corrected the mini wallpaper format.
- Browser widget timers now use a counted lifecycle so an old activity's stop callback cannot pause timers while a new home activity is visible.
- CI and manifest: fork pull requests no longer require signing secrets, pin shortcuts are exported to the system, the proot source hash is verified, stale share-only paths were removed, and the widget signal source has the phone-state permission it needs.

## [2.9.35] - 2026-11-22

### Fixed

- Launcher crash on Android 13 relaunch (e.g. RMX3491): home OWL tiles are now composed only after the native desktop has been measured. Tile geometry also treats an empty grid as 1 pixel instead of calling `coerceIn(1, 0)`.
- Added a JVM regression test for tile geometry with an unmeasured grid.

## [2.9.34] - 2026-11-22

### Added

- The launcher's own Settings window now exposes **system updates**, **fast system calls**, **stay alive in the background**, **packages**, **permissions** and **licenses**. The Linux, PC Update, Privacy and About pages carry cards that open the existing screens, which were implemented but had no entry point from the running launcher.

### Fixed

- Home widgets tick while home is visible: the native activity now calls `homeStarted()` / `homeStopped()`.
- Widget permission prompts follow the native activities through `WidgetAsks` on resume and pause.
- Browser WebView timers follow home visibility through `BrowserPages.started(...)`.

### Removed

- Launcher backup/restore logic and its Settings card. Platform backup rules are unchanged.
- Widget sharing: QR/link export and import, store publish, shared-widget agent commands, share strings, docs and the ZXing dependency. File sharing and the normal widget store remain.

## [2.9.33] - 2026-11-22

### Added

- The guest-side `bellatrix` command is installed again. Agents and scripts in Void Linux can query phone location, calendar, AirPods, battery, notifications, folders, and share/copy files through the launcher bridge.
- Full license texts ship with the app, so Settings → Licenses opens the real notices.
- The guest gets a phone-sized `fastfetch` config, themed fallbacks for foot, btop, Pi and Claude Code, and a small Bellatrix wordmark for the fastfetch logo.
- Tests cover the `bellatrix` command contract and the fastfetch config adaptation.

### Fixed

- Without **All files access**, the guest still sees the launcher-owned shared storage at `/sdcard`, so `bellatrix ls/get/put/pick` and `/sdcard` paths keep working under scoped storage.
- Terminal font family and size in `style.toml` now actually reach `foot`, including on first start.
- Icon packs use the shared appfilter parser: activity-specific icons win, activity aliases fall back to the app icon, and density-specific drawables are used. Work/profile rows pass their launcher activity to the pack.
- The eight translated locales have the missing widget/drawer/lock/voice strings again.

## [2.9.32] - 2026-11-22

### Fixed

- **Lock/Unlock** in the native widget menu now toggles in place immediately, both ways, without reopening the launcher. The tile's grip also disappears or returns on the same tap.
- The resize corner has no visible box or handle any more. Its bottom-right hit area is still there, but nothing is drawn on top of the widget.
- The Coding Agents panel header now shows only **Coding Agents**. The overlapping **Choose an agent** subtitle and settings gear are gone.
- `android.device` and the fetch widget are back to Omalauncher's exact behavior: `{model, maker, android, sdk, soc}` from `Build.*`, with no synthesized labels or extra fields.
- The launcher icon is generated from `/sdcard/Bellatrix.png`.

## [2.9.31] - 2026-11-22

### Added

- The taskbar's **Widget Zone** is now the **Coding Agents** picker. It lists installed agents with their logos, marks the current default, and choosing one makes it the default and opens it. Its taskbar icon is now a dedicated coding-agents icon.

### Changed

- Coding Agents no longer appear in the app drawer or the app-drawer filter. Install and remove them from the standalone Coding Agents settings page or from the picker's **Manage agents** button.

### Fixed

- Widget drag no longer blinks, trails, or feels heavy. Position is applied in Compose's placement phase without remeasuring the OWL tile, and movement uses Compose's own per-event delta so the tile follows the finger instead of lagging a frame behind.
- The bottom-right resize grip is now decisive: pressing it starts resizing immediately, its touch band matches the visible grip, and it is no longer mistaken for a body drag.
- The app drawer's old Recommended reservation is gone; the app grid now fills the available space between the title and footer instead of leaving 318 dp of dead space.
- Floating terminal pixels now render through a `TextureView`, so raising a terminal puts its content above ordinary Views. Coding Agents can no longer cover the terminal's text.

## [2.9.30] - 2026-11-22

### Added

- **Lock widget** in the native tile popup. A locked tile keeps its exact pixel rectangle, still opens on tap, but cannot be moved or resized; its resize grip is hidden. The same menu item becomes **Unlock widget**. The lock persists per widget across restarts.

### Changed

- Moving or resizing a widget now requires a deliberate hold first. A quick swipe is ignored instead of dragging the tile, so tapping a widget (or its controls) no longer turns into an accidental move. Hold the tile, then drag; or hold and release to open the popup menu.

### Fixed

- Widget drag/resize is now applied in one Compose layout pass instead of separate offset and size passes. That removes the one-frame size/position mismatch that showed up as judder or blinking while moving a tile.
- The native home overlay still avoids recomposing OWL content on every pointer event; only the tile's layout node is invalidated during a drag.

## [2.9.29] - 2026-11-22

### Added

- `android.device` now exposes `label`, `brand`, `device`, `product`, `board` and `hardware`. Missing Android identity fields are null, and `label` is the first real name the phone reports. The fetch widget uses it for its host line, so a generic `MODEL` no longer leaves the device blank.

### Changed

- Native home tiles now have exactly one resize grip, at the bottom-right corner. Drag it right or down to grow, left or up to shrink; the tile body still moves the widget. The edge/corner dot ring is gone.
- Drag geometry is held in the layout/offset phase instead of the widget's composition phase, so moving or resizing no longer rebuilds the OWL scene on every pointer event.
- Documentation now describes the `.owl` file workflow as Bellatrix's Linux-style ricing loop: edit a plain file, preview with `owl-check`, and drive live state with `owl-send`, while the native shell stays authoritative.

### Fixed

- Resize used the tile's screen origin against pointer coordinates that were already tile-local, so grips missed except near the top-left. The bottom-right grip now uses tile-local coordinates directly.
- Fastfetch-style device detection falls back from model to product, device, board, brand and finally `phone` instead of showing an empty host.

## [2.9.28] - 2026-11-22

### Added

- Holding a widget or face tile opens Bellatrix's own native context-menu `PopupWindow` with **Edit** and **Remove**. There is no always-visible dropdown button and no Compose imitation menu.

### Changed

- Drag moves a tile immediately; a hold-and-drag resizes from the edge or corner under the finger. Resize now uses tile-local coordinates, so every edge and corner works instead of only tiles near the screen origin. Tiles still move and resize freely by pixel, may overlap, and are clipped without a home border.
- Edit still routes to the default Coding Agent, or the standalone Coding Agents page when no agent is installed.

### Fixed

- Coding Agent startup flags are inserted after the agent binary instead of the first shell token. Opening Edit with OpenCode or Kilo no longer runs `cd --standalone ...` and drops into a bare shell.

## [2.9.27] - 2026-11-22

### Added

- Widget and face tiles now have a tile menu with **remove** and **edit**. Remove moves the `.owl` file to `.removed/`; edit starts a Coding Agent session for that file, or opens the standalone Coding Agents page if no agent is installed.
- A tile's starting rectangle is declared on its root `Box`/`Flex`/`Anchor` with `width`, `height`, `size`, `fill`, `offset` and `anchor`. Tiles can overlap, and drag/resize geometry is saved as free pixels rather than grid cells.

### Changed

- Long-press selects a tile; dragging it moves it anywhere, and dragging any edge or corner resizes it freely. The home area is clipped to its real bounds with no visible border line.
- The APK requests 90 Hz where the device offers it, makes hardware acceleration explicit, and lets OWL animations and Linux window compositing run up to 90 fps. OWL 3D scenes were already implemented and were left unchanged.

## [2.9.26] - 2026-11-22

### Added

- Native home widget tiles can be resized by hand: long-press a tile to select it, then drag its bottom-right handle in whole grid cells. The top-right button edits that tile's `.owl` file. The size is saved to `layout.toml` immediately.

### Fixed

- `taskbar.owl` now restyles the real native taskbar views instead of adding a second Compose taskbar on top. The root `Box` properties `background`, `radius`, `border`, `borderColor` and `padding`, a visible `Flex`/`Row`/`Column` `gap` for task-button spacing, and a visible `Text` color for icon tint change the native bar; a transparent file restores Bellatrix's shipped look. This removes the stray oval/press spot caused by the overlay.
- Widget tiles are clipped and clamped to the native home area above the taskbar, with a visible home border, so they cannot escape the home view after rotation or resize.
- The native Liquid Glass **Check for updates** dialog is now dark in both light and dark system modes.

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
