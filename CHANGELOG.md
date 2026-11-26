# Changelog

All notable changes to Bellatrix are documented here. Every GitHub Release body is generated
from the matching section of this file by GitHub Actions, so a `v*` tag without a section fails
the release build instead of shipping notes-free update.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Bellatrix uses
`MAJOR.MINOR.PATCH` tags and publishes signed APKs only from GitHub Actions.

## [Unreleased]

## [3.1.3] - 2026-11-26

### Fixed

- The release build compiles again: a stray closing brace in the Explorer ended its class early, and `$EDITOR` in the Linux agent notes was read as a Kotlin variable.
- Restored the light and dark vector Bellatrix launcher logos in place of the gas-giant planet artwork.

## [3.1.2] - 2026-11-26

### Changed

- The editor is nano. Edit in the Explorer's menu, opening a text file in the Explorer or Files, the Editor entry and `$EDITOR` in Linux (`git commit` and the like) all open `nano` in a terminal window. The built-in touch editor (Editor window, Explorer editor, "Open in micro") is removed. nano is installed in Linux (new installs with the other packages, existing ones by an optional step the next time Linux updates); micro stays installed if you have it.

### Fixed

- Updating from PC Update shows its progress. Download & Install now fills the bar in the page header and the line under the version says "Downloading 45%", instead of only a thin bar sliding back and forth. The APK is streamed to a file in chunks, so the percent comes from the real download size (it says "Downloading…" if GitHub doesn't give one), and a half download is never handed to Android's installer.
- A failed update download or install now says so in a message, instead of the page quietly going back to normal.
- Download & Install says why it opened a screen when "Install unknown apps" isn't allowed yet: a message asks you to allow it and tap Download & Install again.
- Settings → Privacy and Permissions have more room between a card's title and its text, and between the permission cards.
- Pictures you add under Background → Images now appear in the same row as the two default wallpapers, before the add button, instead of in a second row underneath. The row scrolls sideways when there are many.
- Home icons no longer swap, double or vanish by themselves. The icon list claimed stable item ids but never gave any, so every icon had the same id and RecyclerView could hand one icon's view to another position without rebinding it.
- Home's grid no longer rearranges itself. Fitting the cells to the screen could run before home was measured (a 1 x 1 grid), closing every gap between icons and saving that; it now waits for a real size. Turning auto-arrange off now fits the cells to the grid home actually uses, and a drop on a stale cell after the phone turned no longer swaps the wrong icons or crashes.
- Pinning an app that is already on home no longer adds a second icon, which made removing either one remove both.
- Back on the home with nothing open no longer closes the launcher. It used to ask "Press again to exit" and then finish the home, which Android restarted at once: the launcher blinked and came back, and everything running (coding agents, Linux) was gone. Back at the bottom of the home now does nothing.
- The Browser no longer crashes with "Can not perform this action after onSaveInstanceState" when a tab opens just as the window is being saved or closed. Showing, hiding and closing tabs now tolerates that, since tabs are rebuilt anyway.

## [3.1.1] - 2026-11-26

### Fixed

- The power button in the Start Menu opens its menu again: Settings, Shut down, Reload and Install updates. Its click did nothing. Shut down stops Linux and closes the launcher, Reload restarts it, and Install updates opens PC Update.
- Folders in the Explorer show the folder icon again. The icon grid and the details list picked an icon from the file name's extension only, so every folder showed the blank page icon.
- A Linux GUI app such as Vesktop is its own taskbar and Recents entry, with its own icon, instead of sharing the Terminal button. Tapping Terminal used to focus the Vesktop window, so the two could not be open together; now Terminal opens and focuses only terminals, as it already did around coding agents.

## [3.1.0] - 2026-11-26

### Added

- Microphone for Linux apps: PulseAudio's default source is now the phone's microphone ("Microphone"), so voice chat and recording apps in Linux hear you. The microphone runs only while something in Linux records from it (the Android indicator shows then), and uses Android's echo cancelling. It needs the launcher's microphone permission (Settings → Permissions); without it Linux gets silence. `cat /tmp/mic-status.txt` says what it is doing.

### Fixed

- Cliamp opens and plays instead of closing at once. It plays through ALSA and the system had only PulseAudio, so it found no output device and quit. A new optional step ("Connecting ALSA apps to the sound server") installs ALSA's PulseAudio plugin and makes it ALSA's default device in `/etc/asound.conf`; it runs by itself the first time Cliamp is opened. A Cliamp that stops with an error now keeps its window open with the error, instead of vanishing.

## [3.0.2] - 2026-11-26

### Fixed

- Linux sound: the audio track holds 150 ms instead of 100, its thread runs at audio priority, and a read of the pipe that ends inside a sample no longer loses its last bytes.

### Added

- `/tmp/audio-stats.txt` in Linux says what the audio bridge saw in the last two seconds (pipe backlog, waits on PulseAudio, track underruns), for finding where sound breaks up: `cat /tmp/audio-stats.txt` while it does.

## [3.0.1] - 2026-11-26

### Added

- Settings → Personalize → Font: pick the font of the launcher's text and widgets from Android's own or a font file (TTF, OTF, TTC) you add. It is kept in the launcher, needs no Linux, and is written as `font = "file:<name>"` in style.toml.
- `bellatrix apps` in Linux lists what is on home as JSON: the desktop's icons and the taskbar's pinned apps, in the order shown, with their package and the phone's name for the app, and the path of home's layout file. Coding agents can read it; AGENTS.md tells them.

## [3.0.0] - 2026-11-26

### Added

- PC Update now downloads and installs the new release inside the launcher: the main button is **Download & Install**, which fetches the release's APK and hands it to Android's installer (Android asks once to allow installs from Bellatrix). "Buka di Chrome" stays as the fallback beside it, and the shell install through Shizuku/Sui or root is unchanged. A release without an APK still opens its GitHub page.
- Settings → Personalize → Background shows every picture or video you add as a preview under Images, after the two default ones. Tap one to use it, hold it to remove it. It appears as soon as the file is saved.
- `/root/RICING.md` in the Linux system: a map of every file that can be edited to change the launcher (widgets, `style.toml`, home and taskbar layouts, native-ui `look.toml`, `ui.lua` and `keys.toml`, themes and backgrounds, the terminal and tool configs), which ones must not be edited, and the rules that keep an edit from breaking anything. AGENTS.md and CLAUDE.md point to it. It is seeded like the other defaults: updated while unchanged, never brought back once deleted.

### Fixed

- The version shown in Settings → System → About and at the top of PC Update now follows the release (it was stuck at 2.9.18).
- Home's wallpaper is now Android's own wallpaper showing through the transparent window, always. The launcher no longer paints `WallpaperManager.getDrawable()` over it, which showed the default wallpaper on Android 13 and threw on Android 14 and later; Android's documentation says to use the window's wallpaper flag instead.
- Deleting a file from the desktop now deletes it and closes the progress dialog. It called a class that does not exist, so nothing was deleted and the dialog stayed open.

## [2.9.56] - 2026-11-26

### Added

- PC Update has Check and Update buttons for the Linux system. Check looks for updated packages and tools now; Update runs `xbps-install -Suy` for the system's packages, then upgrades the tools mise installed and the coding agents, and checks again. The "System updates" card shows what is happening (updating, checking, how many are waiting, up to date, or why it failed) and opens a drop-down listing every package and tool waiting. The buttons are greyed while a job runs, and Update while nothing is waiting. Until now the update job existed but nothing started it, and the card only reopened the desktop.

## [2.9.55] - 2026-11-26

### Fixed

- Opening a JavaScript file in Explorer's editor no longer crashes. Code is now coloured only on the lines in view (plus a screenful either side) instead of every token in the file, and minified files and files over 250,000 characters open in a plain, wrapped mode without colours or line numbers, because one very wide line stalled and crashed drawing. Files over 1 MB go to another app instead of an edit field.
- Folders Explorer could not tell from files (it showed them with a file icon and sent them to the editor, which gave up and opened a system chooser) are now recognised: an entry that is neither a file nor a folder to `stat` but can be listed is a folder. Tapping anything that is not a regular file now opens it as a folder instead of as data.

## [2.9.54] - 2026-11-25

### Changed

- Explorer's editor now covers the whole Explorer window below its title bar instead of half of it, and gives the window back on close.
- Explorer opens files like a file manager does, by what they are: videos, music and photos in their apps; text and code in the editor; documents, archives, packages, fonts and databases in whichever app takes them; a file with an extension nobody knows is looked into, and text goes to the editor. `.html`, `.htm` and `.svg` open in the editor too ("Open with" still shows them in a browser).
- The editor colours code for its language (Kotlin, Java, Gradle, JavaScript, TypeScript, Python, C, C++, C#, Go, Rust, PHP, Ruby, shell, SQL, Lua, Swift, Dart, CSS, JSON, HTML/XML, YAML, config files, Markdown, diffs), by extension or, for a file without a known name, by its first line (a `#!` line, `<?xml`, `{`). Code is shown unwrapped with line numbers and prose is wrapped; a Wrap button switches between them. Colours follow light and dark mode. Files over 120,000 characters stay plain so typing stays smooth.

## [2.9.53] - 2026-11-25

### Added

- Explorer has its own editor, inside its window: opening a file in Explorer shows it in the file area (back arrow, name with a dot while unsaved, Save, Ctrl+S, Esc), and going to another folder, or opening another file, asks first when there are unsaved changes. Any extension opens as text, with line endings, a UTF-8 byte order mark and a non-UTF-8 encoding kept so saving rewrites nothing that was not edited. Files that are not text or are over 2 MB go to the system's apps instead. "Edit" in an item's menu opens any file this way.

### Changed

- Opening a text file or `.txt` from Explorer no longer goes to Notepad or the separate editor activity. Videos, music, photos, `.htm`, `.html` and `.svg` still open in their own apps.

## [2.9.52] - 2026-11-25

### Fixed

- Tapping a file or folder in Explorer opens it. The listener behind a tap was a stub that did nothing, so only the long-press menu's Open worked.
- Text files and configs without an extension that Explorer opens now open in Notepad's window, which edits and saves in place, instead of the separate full-screen editor (which also needs the Linux runtime and closes without it).

## [2.9.51] - 2026-11-25

### Fixed

- The on-screen keyboard now appears when you tap a text field in Chromium and Electron apps such as Vesktop. They turn text input on but report an empty cursor rectangle, which Bellatrix read as "no text cursor"; a tap anywhere in a window with text input on now counts as a tap on text. Terminals that report a real cursor row behave as before.
- Bellatrix now answers every text-input commit with `done`, as `zwp_text_input_v3` requires and Chromium waits for, and no longer drops a cursor rectangle sent before `enable`.

## [2.9.50] - 2026-11-25

### Fixed

- Music and Photos no longer open with "Failed to open file" / "File not found". Opening an app with no file reached the window as an empty path, and Music tried to play it and Photos looked for it. An empty extra now means no file.
- Explorer's long-press menus work: on an icon (Open, Edit, Open with, Cut, Copy, Paste, Rename, Delete, Share, Select all) and on empty space (New folder, Paste, Select all). The operations behind them were already ported, but both menus were empty stubs, so Explorer could only browse.

## [2.9.49] - 2026-11-25

### Changed

- Home's icons now fill down the first column from the top left, then continue at the top of the next column. A new app takes the first empty cell in that order. Saved layouts are packed once (same order, gaps closed) so they start at the top left.

- Opening Terminal (or a Linux app) before Linux is installed now asks "Install Linux?" in a liquid glass card with Install / Not now, instead of jumping into Settings. The card blurs what is behind it (Android 12+), lights its edge with a shader that follows the phone's tilt and your finger (Android 13+), and springs in and out.
- The app icon is now a gas-giant planet, centre-cropped 1:1 from the supplied artwork, as an adaptive icon plus sharp legacy icons.

### Fixed

- Long-pressing an icon on home always opens its menu. The icon's drag now starts from that long press instead of racing it on a separate timer that could cancel the menu, and moving an icon no longer leaves the rearrange flag on, which made every long press on home and the taskbar do nothing.
- Turning the phone no longer scrambles home: the icon list keeps its order and only re-wraps, empty cells are rebuilt for the new screen on every layout change, and gaps close up first when the new orientation has fewer cells so no icon ends up off screen.
- Floating windows follow the screen when the phone turns. The screen size is measured again whenever the window layer changes, open floating windows are scaled to the new shape, and a window can no longer be sized or placed past the taskbar.
- Home's desktop icons open, long-press and drag again. The page-swipe handler added in 2.9.47 sat on a full-size Compose layer over the desktop and swallowed every touch before the icons saw it; sideways page swipes are now read from the native desktop without consuming the touch.

## [2.9.48] - 2026-11-24

### Added

- The drawer and home tile menus have **Open in window**. Owl apps and guest Linux apps open in Bellatrix's own resizeable window layer with title bar, taskbar entry and normal lifecycle; Android apps ask the system for a freeform window. Widgets can offer the same action with `appAction("window", key)` (owl 25).
- Linux app discovery also reads `/usr/local/share/applications`, Flatpak exports, and AppImages in `~/Applications`, `~/Downloads` and `/opt`, so an installed Vesktop appears in the drawer and can be pinned to home. Electron/Chromium guest apps get Wayland flags and a `Path=` working directory when their desktop entry needs one.

### Fixed

- A second tap on an already-open Linux app focuses its in-home window instead of trying to bring forward a task that doesn't exist.
- Guest windows follow `look.toml` window rules like other app windows, and keep their title, resize and close controls for GUI apps as well as terminals.

## [2.9.47] - 2026-11-24

### Added

- Global key chords are configured in `~/.config/bellatrix/native-ui/keys.toml`: bind Super, Ctrl, Alt and Shift combinations to Terminal, Files, Settings, Linux settings, Coding Agents, Voice or close-window. A bare key is never bound, so typing is untouched.
- `look.toml` now accepts `[[window-rule]]` blocks to set gaps, size and floating/maximized state for Bellatrix's own app windows.
- Home has workspaces again: `[[page]]` blocks in `layout.toml` are separate pages, swipe sideways on home to change page, and `page:` in an owl file places its tile. Saved layouts from the one-page version keep working.

### Fixed

- Music, video and Photos now understand guest paths, the Android paths behind them and content Uris, instead of treating every one as a plain file and reporting it missing.
- Files hands documents to Android through the FileProvider authority the app actually declares, with a write grant for apps that edit in place.
- Files opened from the desktop editor no longer stop at a thumbnail and a name: text of every extension is editable, and binary files open as a byte-preserving view that writes the same bytes back.

## [2.9.46] - 2026-11-24

### Fixed

- Home's icon grid stays fixed on the screen; dragging a finger on it no longer scrolls the desktop up and down.

## [2.9.45] - 2026-11-24

### Fixed

- UiPhotos now finds pictures already in the phone's storage. With photo access granted it shows a thumbnail grid from Android's media library, and the Browse button opens the system photo picker for files that are not indexed yet.
- With the taskbar set to the left, the app drawer now opens from the bottom-left corner as well instead of staying on the right.
- Fixed a crash when opening the music player: the navigation bar no longer casts its toggle to the wrong button type while inflating.

## [2.9.44] - 2026-11-24

### Fixed

- Bellatrix's home now shows the phone's own wallpaper instead of a stale saved copy. A still picture picked in Background becomes the system wallpaper, and a wallpaper changed from Android Settings appears in Bellatrix right away.
- Video wallpapers keep the Video type selected after they are applied. Video sound now comes from one player, so it is no longer doubled.
- Added a wallpaper volume slider for video wallpapers in Settings → Personalize → Background.

## [2.9.43] - 2026-11-24

### Added

- The existing Settings → Personalize → Background picker now accepts any file: images, animated images and video formats are detected from the file itself instead of a short MIME allowlist. The picked clip is also saved as a theme background, so Bellatrix's live wallpaper can keep showing it in other formats or when the launcher is not in front. Media3 exports still produce the trimmed video pair when supported; other containers fall back to the original file.
- Shell-level app install through Shizuku or Sui, exposed from the existing PC Update dialog. Root, ADB shell, USB debugging and Android 11+ wireless debugging all reach the same privileged service; the launcher asks for shell permission in the same Settings surface and shows the current access state there.

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
