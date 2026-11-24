# Bellatrix native UI scripting

Three files in this directory let a coding agent change Bellatrix's **own** UI from Linux:

| File | What it owns |
| --- | --- |
| `look.toml` | Declarative style for existing native `View`s: size, padding, background, border, text, icon, visibility, and window geometry |
| `ui.lua` | Behaviour: click/long-click/gesture hooks, adapter data, taskbar actions, Settings navigation, window decorations |
| `keys.toml` | Global key chords on Bellatrix's home screen and windows |

Guest path: `~/.config/bellatrix/native-ui/`. Errors from all three files go to `errors.txt` beside them.

Both shipped files are neutral templates: a fresh install keeps the stock Bellatrix look and behavior.
Save either file and the change reaches the views already on screen within a second. A file that does not
parse is ignored and reported, never overwritten. Deleting `look.toml` restores the stock look; deleting
`ui.lua` restores directly edited views and adapter data and removes every Lua rule and hook. There is no second launcher, Activity, home, drawer, Settings
window, renderer or mode toggle. Every change lands on a Bellatrix `View` that is already on screen.

`examples/taskbar.lua` and `examples/settings.lua` are active examples you can copy into `ui.lua`.

Deleting `keys.toml` removes every global binding; no key is bound on a fresh install.

## `look.toml`

```toml
version = 1

[palette]
surface = "#CC101418"
border = "#AA00AAA4"
text = "#BDF5EF"

[[rule]]
surface = ["taskbar", "start-menu"]
match = ["id:taskBar", "id:startMenuBg"]
description = "Optional note for people reading the file."
background = "surface"
border = "border"
border-width = 2
radius = 15
padding = [8, 6]
height = 56
```

`palette` holds reusable colors: `#rgb`, `#rrggbb` or `#aarrggbb`. A rule may use a palette name in any
color property. `none` or `transparent` clears a color.

`[[rule]]` blocks are applied in file order; later rules win. Every rule has:

- `surface`: one surface name or an array. `all` means every native surface.
- `match`: one selector or an array. A view matches when any selector matches. Omitting it means `all`.
- `description`: ignored by the launcher; a note for the next editor.

Unknown keys are reported in `errors.txt` and skipped.

### Surfaces

| Name | Native views it covers |
| --- | --- |
| `home` | MainActivity's home, desktop grid and its native tiles |
| `taskbar` | The real taskbar and its item views |
| `start-menu` | The native start menu, its search, app grid and suggestions |
| `action-center` | Quick settings, calendar popup, Widget Zone and more-icons popup |
| `settings` | Bellatrix Settings cards, switches, spinners and search |
| `window` | Native app windows and their title/toolbar views |
| `drawer` | Native app-drawer views inside the start menu |
| `dialog` | Other native dialogs and popups |

A view inside a nested surface uses that surface: text in the taskbar is `taskbar`, even though the
activity is `home`.

### Selectors

| Selector | Matches |
| --- | --- |
| `id:taskBar` | A view's resource id, e.g. `taskBar`, `startMenuBg`, `settings_content`, `title_bar` |
| `class:UISettingsCard` | A view class name, or a prefix/suffix of its simple name; `*` is a wildcard |
| `text:Wi-Fi` | The exact text of a `TextView`; `*` is a wildcard |
| `all` | Every view in the surface |

To discover ids, read the layouts in this launcher's source at `app/src/main/res/layout/`. The same names
are accepted here; only views in the existing Bellatrix view tree are addressable.

### Properties

Colors accept a palette name, `#rgb`, `#rrggbb` or `#aarrggbb`.

| Property | Effect |
| --- | --- |
| `background` | Solid panel fill; with `radius` it becomes a rounded drawable |
| `border`, `border-width` | Stroke color and width |
| `radius` | Corner radius; clips the view unless `clip = false` |
| `padding`, `margin` | One value, two values (vertical, horizontal), or four (left, top, right, bottom) |
| `width`, `height` | Size in dp, `sp`, `px`, `match`, `wrap` or a percentage of the parent |
| `min-width`, `min-height` | Minimum size |
| `alpha`, `visibility` | `0`–`1`; `visible`, `invisible` or `gone` |
| `elevation`, `translation-x`, `translation-y`, `rotation`, `scale-x`, `scale-y` | View transform |
| `text-color`, `hint-color`, `text-size`, `text-style`, `text-align` | `TextView` styling; style is `normal`, `bold`, `italic` or a combination |
| `icon-size`, `icon-color`, `icon-tint` | `ImageView` size and tint |
| `clip`, `clickable`, `focusable` | Boolean view flags |
| `layout-gravity`, `weight` | Linear/frame layout gravity and linear weight |

A bare number is dp, except `text-size`, where it is sp. Sizes may also be strings: `8dp`, `12sp`, `3px`,
`50%`, `match`, `wrap`.

### Window geometry

`[[window-rule]]` blocks in `look.toml` set where Bellatrix's own app windows open. `match` takes one or more
window class names (`UIMusic`, `UIExplorer`, `UI*`) and `*` is a wildcard. The first matching rule wins.

```toml
[[window-rule]]
match = ["UIMusic", "UIPhotos"]
gaps = 12
width = "70%"
height = "80%"
state = "floating"
```

| Property | Effect |
| --- | --- |
| `gaps` | dp kept on every side; the window is inset and nudged away from the screen edge |
| `width`, `height` | Size as dp, px, or a percentage of the screen; `match` fills, `wrap` keeps the default |
| `state` | `floating`, `maximized` or `fullscreen` |

Wayland guest windows are placed by the compositor; these rules apply to the launcher's native windows
(music, video, Photos, Files, Browser, Settings, Notepad, …).

## `keys.toml`

Global chords are listed one per `[[keybind]]`. A chord needs at least one modifier (`Super`, `Ctrl`, `Alt`,
`Shift`) plus a letter, digit or named key (`Esc`, `Enter`, `Tab`, `Space`, `F1`–`F12`, arrow names). A bare
key is never bound, so typing in a terminal or editor is untouched. Keys are caught on Bellatrix's home and
in its windows; an app outside the launcher keeps its own keys.

```toml
version = 1

[[keybind]]
combo = "Super+T"
action = "terminal"

[[keybind]]
combo = "Super+Shift+F"
action = "files"
```

Actions: `terminal`, `files`, `settings`, `linux-settings`, `coding-agents`, `voice`, `close`.
`close` closes the focused Wayland window, when there is one. Bad combos and unknown actions are reported in
`errors.txt` and skipped.

## `ui.lua`

Lua runs inside Bellatrix's one `App.runtime`. It is the same launcher process and the same view tree.
The global object is `nativeui`; every call takes a selector string and a surface name from the tables
above. Call its methods with `:` (`nativeui:style(...)`), and do the same for view and adapter handles.
Halting the script or deleting it removes its rules and hooks, not the launcher.

### Selector and view helpers

| Call | Effect |
| --- | --- |
| `nativeui:views(match, surface)` | Lua list of handles for every matching native view |
| `nativeui:view(match, surface)` | First matching handle, or `nil` |
| `nativeui:style(match, surface, props)` | Adds a rule whose keys are `look.toml` properties; Lua lists work for `padding`, `margin` and `border` |
| `nativeui:setText(match, surface, text)` | Sets text on matching `TextView`s |
| `nativeui:setHint(match, surface, hint)` | Sets hint on matching `TextView`s |
| `nativeui:setBackground(match, surface, color, radius)` | Sets a solid or rounded drawable |
| `nativeui:setPadding(match, surface, left, top, right, bottom)` | Sets padding in pixels |
| `nativeui:setVisibility(match, surface, value)` | `visible`, `invisible` or `gone` |
| `nativeui:adapter(match, surface)` | Handle for an existing native `AdapterView`/`RecyclerView` |

A view handle has `id()`, `surface()`, `className()`, `text()`, `setText()`, `setHint()`, `background(color, radius)`,
`border(color, width)`, `padding(l, t, r, b)`, `width(px)`, `height(px)`, `visibility(value)`, `alpha(value)`,
`iconSize(px)`, `tint(color)` and `raw()` (the underlying native `View`). Example:

```lua
local bar = nativeui:view("id:taskBar", "taskbar")
if bar then bar:background("#CC101418", 14) end
```

### Hooks

`onClick`, `onLongClick` and `onGesture` take `(match, surface, callback)`. The callback receives
`(view, action, x, y)`. `action` is `click`, `long-click`, or a gesture phase: `down`, `move`, `up`,
`cancel`. Hooks observe the native touch; they do not consume it or replace Bellatrix's own click and
long-click actions.

```lua
nativeui:onGesture("id:taskBar", "taskbar", function(view, action, x, y)
    if action == "down" then view:alpha(0.85)
    elseif action == "up" or action == "cancel" then view:alpha(1.0) end
end)

nativeui:onLongClick("class:UISettingsCard", "settings", function(view, action, x, y)
    nativeui:log("held " .. view:id())
end)
```

### Adapter data

`nativeui:adapter(match, surface)` returns a handle for the adapter already installed on that native
list. `count()`, `data()`, `textAt(i)` and `viewAt(i)` read it. `setData({"one", "two"})` replaces the data
only when the native adapter is a string `ArrayAdapter`; other adapters stay owned by Bellatrix and their
rows can be restyled through `viewAt(i)`. `notifyChanged()` redraws it.

### Native actions

| Call | Effect |
| --- | --- |
| `nativeui:taskbar("pin", "UISettings")` | Adds a pin through the real taskbar `layout.toml` |
| `nativeui:taskbar("unpin", "UISettings")` | Removes that pin |
| `nativeui:taskbar("refresh")` | Re-applies the current pins |
| `nativeui:taskbar("launch", "UISettings")` | Opens a native Bellatrix window |
| `nativeui:launch("UIExplorer", "path")` | Opens any native window key; data is optional |
| `nativeui:pins()` | Current taskbar pin keys |
| `nativeui:settings("1")` | Opens the existing Bellatrix Settings page; `""` opens its root and `"1"` is System |
| `nativeui:window("title", "Title")` | Sets the current native window decoration's title |
| `nativeui:window("background", "#CC101418")` | Recolors the native `title_bar` |
| `nativeui:window("height", "56")` | Sets the native title bar height in pixels |
| `nativeui:log(text)`, `nativeui:toast(text)` | Log to logcat, or show a short native toast |

## Limits

This system edits Bellatrix's existing native views. It cannot install a different theme engine, add a
second home or replacement renderer, change Android's system bars, or alter Linux/Wayland services. Calling
the API from a file in this directory never needs a source edit or an APK rebuild.
