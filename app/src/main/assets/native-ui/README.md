# Bellatrix native UI overrides

`look.toml` in this directory changes Bellatrix's **own** views: the taskbar,
start menu, action center, Settings cards, app windows and home tiles. A rule
finds a real `View` in those existing screens and changes its size, padding,
background, border, text, icon or visibility in place. There is no second
launcher, no replacement screen and no theme picker.

Guest path: `~/.config/bellatrix/native-ui/look.toml`.
Errors go to `~/.config/bellatrix/native-ui/errors.txt`.

Save the file and the next layout pass restyles the views already on screen;
opening a panel or dialog also picks the change up. A file that does not parse
is ignored and reported, never overwritten. Deleting the file brings back the
version shipped with this launcher.

## Format

```toml
version = 1

[palette]
surface = "#CC001811"
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

`palette` holds reusable colors: `#rgb`, `#rrggbb` or `#aarrggbb`. A rule may
use a palette name in any color property. `none` or `transparent` clears a
color.

`[[rule]]` blocks are applied in file order; later rules win. Every rule has:

- `surface`: one surface name or an array. `all` means every native surface.
- `match`: one selector or an array. A view matches when any selector matches.
  Omitting it means `all`.
- `description`: ignored by the launcher; a note for the next editor.

Unknown keys are reported in `errors.txt` and skipped.

## Surfaces

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

A view inside a nested surface uses that surface: text in the taskbar is
`taskbar`, even though the activity is `home`.

## Selectors

| Selector | Matches |
| --- | --- |
| `id:taskBar` | A view's resource id, e.g. `taskBar`, `startMenuBg`, `mainView` |
| `class:UISettingsCard` | A view class name or its suffix; `*` is a wildcard |
| `text:Wi-Fi` | The exact text of a `TextView`; `*` is a wildcard |
| `all` | Every view in the surface |

To discover ids, read the layouts in this launcher's source at
`app/src/main/res/layout/`. The same names are accepted here.

## Properties

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

A bare number is dp, except `text-size`, where it is sp. Sizes may also be
strings: `8dp`, `12sp`, `3px`, `50%`, `match`, `wrap`.

## Examples

Move the taskbar's native app cluster and restyle it:

```toml
[[rule]]
surface = "taskbar"
match = "id:taskBarList"
background = "#CC101418"
radius = 18
padding = [6, 10]
```

Give native Settings cards a denser look:

```toml
[[rule]]
surface = "settings"
match = "class:UISettingsCard"
background = "#20FFFFFF"
border = "#40FFFFFF"
border-width = 1
radius = 8
margin = [2, 0]
padding = [10, 12]
```

Hide one native tray icon without hiding the taskbar:

```toml
[[rule]]
surface = "taskbar"
match = "id:tray_battery"
visibility = "gone"
```

## Limits

Only properties the launcher understands can change. This file cannot install
a different theme engine, add a second home, replace Android's system bars or
change Linux/Wayland services. It applies to Bellatrix's native views only.
