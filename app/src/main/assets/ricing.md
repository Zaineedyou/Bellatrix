# Ricing Bellatrix: what you can edit, and how not to break it

This file lives at `/root/RICING.md`. Everything below is a plain file under `/root` (the guest's home). The launcher
watches them: **save, and the change reaches the phone within about a second.** There is no restart, no second
launcher and no mode switch: you change the views and files Bellatrix already uses.

Every file in this list that the launcher reads has a neighbour called `errors.txt` that says what was ignored and
why. Read it after every edit.

## 1. The map

Paths are under `~/.config/bellatrix/` unless they start with something else.

| Path | What it controls | Format |
| --- | --- | --- |
| `widgets/widget-*.owl`, `widgets/face-*.owl`, `widgets/*.owl` | Home tiles, faces, drawer apps (`app "x" { }`) and swipe panels (`panel "x" { }`) | OWL |
| `widgets/home.owl` | Skin drawn over the real home area. Starts transparent | OWL (`panel`) |
| `widgets/taskbar.owl` | Restyles the real native taskbar views. Starts transparent | OWL (`panel`) |
| `widgets/drawer.owl` | The app drawer (a panel). Edit it to change the drawer | OWL (`panel`) |
| `widgets/keybar.owl` | The extra keys over the soft keyboard in a terminal | OWL (`keybar`) |
| `widgets/README.md`, `widgets/docs/` | The OWL language reference. **Read only**: rewritten by the launcher on every update | Markdown |
| `widgets/examples/` | Widgets to learn from. Not loaded until you move one up into `widgets/` | OWL |
| `widgets/.stock-drawer.owl`, `.stock-keybar.owl` | The drawer and key bar as shipped, to compare with or fall back to. **Do not edit**: rewritten on every update | OWL |
| `style/style.toml` | Theme, background, tile gaps and borders, corners, glass, system-bar colors, terminal font, icon pack, swipe gestures | TOML |
| `style/defaults.toml` | Every `style.toml` setting with its default and range. **Reference only** | TOML |
| `style/errors.txt` | What `style.toml` ignored | text |
| `home/layout.toml` | What is pinned on home: apps, folders, widget tiles | TOML |
| `taskbar/layout.toml` | The taskbar's pins | TOML |
| `native-ui/look.toml` | Declarative changes to native views (size, padding, background, border, text, icon, visibility) | TOML |
| `native-ui/ui.lua` | Behaviour: click, long-press and gesture hooks, taskbar pins, Settings navigation, window decorations | Lua |
| `native-ui/keys.toml` | Global key chords on home and in windows | TOML |
| `native-ui/README.md`, `native-ui/examples/` | Selector, property and API reference, and working examples | Markdown, Lua |
| `themes/<name>/colors.toml` | A theme's palette (see section 4) | TOML-like |
| `backgrounds/<name>/` | Extra pictures and videos for the theme `<name>` | image, video |
| `~/.config/foot/foot.ini` | The terminal | ini |
| `~/.config/fastfetch/config.jsonc` | fastfetch | JSONC |
| `~/.config/bellatrix/branding/about.txt` | The text fastfetch shows as "about" | text |
| `~/.config/opencode/opencode.json`, `tui.json` | opencode | JSON |
| `~/.config/gtk-3.0/` and `gtk-4.0/` (`settings.ini`, `gtk.css`) | GTK apps | ini, CSS |

The last five rows are defaults the launcher wrote once. Edit them freely: the launcher updates one only while it
is still unchanged, and never puts it back after you delete it.

### Do not edit

- `~/.local/state/bellatrix/current/` (`theme/`, `theme.name`, `background`). It is regenerated from the current
  theme every time the theme changes. Edit the theme in `themes/<name>/` instead.
- `/usr/local/bin/owl-check`, `/usr/local/bin/owl-send` and the `btop` wrapper in `/usr/local/bin`. They are rewritten
  by the launcher.
- The `docs/` folder and `README.md` in `widgets/`, and `.stock-*.owl`. Rewritten on update.

## 2. Rules that keep things from breaking

1. **Edit, save, then look.** After saving, read the `errors.txt` next to the file. For `.owl` files run
   `owl-check <file>`: it prints the errors and the path of a PNG of the result drawn with live data. Open the PNG.
2. **A file that does not parse is ignored, never overwritten.** The launcher keeps the last good state and waits for
   you to fix the file. Fix the first error listed and save again.
3. **One change at a time.** If several things changed and something is wrong, you cannot tell which.
4. **Back up before a big edit.** `cp file file.bak` next to it. Do not name a backup with a `.owl` extension in
   `widgets/`: every `.owl` there is loaded as a real widget.
5. **To take a widget off home, move its file into `widgets/.removed/`** instead of deleting it.
6. **Do not use symlinks** for theme files or backgrounds. Symlinked files and folders there are skipped.
7. **Keep `version = 1`** at the top of `layout.toml`, `look.toml` and `keys.toml`.
8. **TOML subset.** The launcher reads `[section]`, dotted bare keys, strings, numbers, booleans, arrays (several
   lines are fine), one-line inline tables and `[[name]]` blocks, with comments. These are errors: quoted keys,
   multi-line strings, dates, and deeper nesting such as `[[a.b.c]]`.
9. **A `home/layout.toml` without any `[[page]]` is ignored** ("no [[page]]; home stays as it is"). Never leave it as
   only `version = 1` while you expect it to change something.
10. **Do not add a second home, theme picker, drawer or mode switch.** Change the Bellatrix views that exist.
11. **The terminals have no systemd, cron, Docker or kernel modules.** Install packages with `xbps-install -S <name>`.
    Anything that needs a service manager will not run.
12. **GUI changes you make on the phone win over the file** for things like a dragged tile's rectangle: the phone
    saves a free pixel rectangle that wins over the file on the next load.

## 3. `style.toml`

Keys are `section.name`, for example `widgets.gaps-in`. Write `[widgets]` then `gaps-in = 8`, or the dotted form.
Value types, and what happens when one is wrong (the setting is ignored and `errors.txt` says why):

| Type | Takes |
| --- | --- |
| flag | `true` or `false` |
| number | a number inside the range `defaults.toml` shows |
| count | a whole number inside the range |
| text, name | a string in quotes |
| color | `"theme"`, a theme color name (`"accent"`, `"red"`, `"color4"`, …) or `"#rrggbb"`; some also take `"none"` |

Settings that exist today: `theme`, `background`, `corners.scale`, `glass.blur`, `glass.opacity`, `widgets.*` (`border`,
`border-focused`, `border-width`, `frost`, `gaps-in`, `gaps-out`), `drawer.*` (`background`, `border`, `border-width`,
`opacity`, `owl`), `screens.*`, `keybar.*`, `status-bar.shown`, `terminal.font`, `terminal.keyboard-suggestions`,
`terminal.text`, `icons.pack`, `icons.monochrome`, `home.*`, `gestures.swipe-up`, `gestures.swipe-down`.
`defaults.toml` is the authority: if it disagrees with this list, it is right.

`home.empty-page-left`, `home.empty-page-right` and `home.home-page` are deprecated: they are read so old files still
load, and cleared. Do not rely on them.

`terminal.font` takes a family name. `fc-list :spacing=100` lists the monospace fonts installed.

Valid color names: `background`, `foreground`, `accent`, `cursor`, `muted`, `selection`, `selection_background`,
`selection_foreground`, `light_foreground`, `bright_foreground`, `dark_foreground`, `lighter_background`,
`dark_background`, `darker_background`, `red`, `green`, `yellow`, `blue`, `magenta`, `purple`, `cyan`, `orange`,
`brown`, the `bright_` form of the first seven, and `color0` to `color15`.

## 4. Themes

A theme is a folder: `themes/<name>/colors.toml`.

- `<name>` must match `^[a-z0-9_][a-z0-9._+-]*$`: lowercase letters, digits, `_`, `.`, `+`, `-`, not starting with
  `.`, `+` or `-`. A theme with another name is skipped.
- `colors.toml` is read line by line as `key = "#rrggbb"`. **The value must be in quotes** and a six-digit hex color
  (`#` or `0x` are fine). A line that does not look like that is dropped silently, not reported. An older
  `alacritty.toml` in the same folder is read when there is no `colors.toml`.
- You do not have to list everything. Missing colors are derived: `background` from `color0`, `foreground` from
  `color7`, the named colors from `color1` to `color6` and `color9` to `color14`, and so on. Define at least
  `background`, `foreground`, `accent` and `color0` to `color15` for the best result.
- `mode = "light"` or `"dark"` (or `theme_type`). It is guessed from the background's brightness when missing.
- Any other **top-level** file in the theme folder that is not a picture and does not start with `.` is copied to
  `~/.local/state/bellatrix/current/theme/` for Linux apps. The launcher generates `foot.ini` and `btop.theme` from
  the colors only when the theme does not supply its own; it also installs a theme's `claude.json` and `pi.json`
  into those tools.
- Shipped themes cannot be changed in place: a theme with the same name in `themes/` overrides the shipped one file
  by file.
- Pictures and videos for the theme go in `backgrounds/<name>/`, not in the theme folder. Pick one with
  `background = "file.png"` in `style.toml`. Still pictures become Android's own wallpaper; a video or an
  animated image (`gif`, `webp`, `apng`, `heic`, `heif`, `avif`) is played by Bellatrix's live wallpaper, which Android
  asks the person to confirm once.

To test a theme, set `theme = "<name>"` in `style.toml`. If no theme has that name, `style/errors.txt` says
`theme: no theme named "<name>" in ~/.config/bellatrix/themes`, and a `background` that is not one of the theme's
backgrounds is reported the same way and the theme's first one is shown. Folder names under `themes/` are the names.

## 5. Widgets, home and taskbar

- Widgets, faces, apps, panels and key bars are OWL. Read `widgets/README.md` and `widgets/docs/` first: they list
  every node, property and handler. Anything not documented there does not exist.
- `owl-send <file> set <state> <value>` and `owl-send <file> refresh` drive a running widget from scripts and hooks.
- `home.owl` and `taskbar.owl` start transparent so the shipped look stays. To change the native bar, set an opaque
  `background`, a `radius`, `border`/`borderColor`, `padding`, a visible `gap` on a `Flex`/`Row`/`Column`, or a visible
  `Text` color. Lay modules out with `Flex`, `Box`, `Anchor` and `anchor` (owl 24).
- A new `widget-*.owl` or `face-*.owl` takes its starting rectangle from its root `width`, `height`, `size`, `fill`,
  `offset` and `anchor`. Tiles can overlap. `page:` in an owl file does not create pages.
- `home/layout.toml` and `taskbar/layout.toml` are edited both by you and by the launcher (pinning on the phone writes
  them). Taskbar names are `UIExplorer`, `UISettings`, `UITerminal`, `linux:<desktop id>` or an Android package.
  Pinning an Android activity for another profile looks like `com.android.chrome/.Main@10`. `docs/home.md` in
  `widgets/` has the full format.

## 6. Native views: `look.toml`, `ui.lua`, `keys.toml`

- `look.toml` rules (`[[rule]]`) choose a `surface` (`home`, `taskbar`, `start-menu`, `action-center`, `settings`,
  `window`, `drawer`, `dialog`, or `all`) and a `match`: `id:<resource id>`, `class:<view class>`, `text:<exact text>`
  or `all`, where `*` is a wildcard. Later rules win. Unknown keys are reported and skipped. `[[window-rule]]` blocks
  set where Bellatrix's own app windows open. Delete `look.toml` to get the stock look back.
- `ui.lua` runs inside the launcher's one runtime through `nativeui`. Delete it to remove every Lua hook and rule.
- `keys.toml` actions are `terminal`, `files`, `settings`, `linux-settings`, `coding-agents`, `voice` and `close`.
  Modifiers are `super`/`meta`, `ctrl`/`control`, `alt`, `shift`. These are Bellatrix keys only: a key pressed in
  another app stays with that app. Nothing is bound on a fresh install, and deleting the file removes every binding.
  The file is `native-ui/keys.toml`, not directly under `~/.config/bellatrix/`.
- `native-ui/README.md` lists every selector, property and API call. Use it; do not guess names.

## 7. If something is wrong

| Symptom | Do |
| --- | --- |
| Nothing changed | Read the folder's `errors.txt`. The file may not parse, so the last good state is still shown |
| A widget is blank or wrong | `owl-check <file>` and open the PNG it prints |
| The look is broken after a style edit | Remove the last lines you added from `style.toml` and save. Deleting a setting restores its default |
| Native views look wrong | Delete `look.toml` (stock look) and `ui.lua` (no hooks), then add rules back one at a time |
| A key does nothing | Check `native-ui/errors.txt`. Home and windows must be in front; other apps get their own keys |
| A theme is missing | Check the name against the pattern in section 4 and that `colors.toml` values are in quotes |
| A shipped default is gone | `widgets/examples/` and `widgets/.stock-*.owl` still hold the shipped ones to copy back |

The launcher's source is at https://github.com/Zaineedyou/Bellatrix, if you need to see how a file is read.
