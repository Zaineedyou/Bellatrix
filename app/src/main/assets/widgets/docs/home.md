# Home and scripts

Part of the widget docs: README.md, one folder up, comes first, and paths here are from the widgets folder.

## Home's layout

Bellatrix home has workspaces: one or more pages of tiles. Each `[[page]]` in
`~/.config/bellatrix/home/layout.toml` is a page, left to right; swipe sideways on home to move between
them, and a dot row shows where you are. `face "name" { }` files can be ordinary tiles or fill a page on
their own. A `page:` in an owl file places its tile on that page unless a page saved on the phone wins.

A widget tile's initial rectangle comes from its own root `Box`/`Flex`/`Anchor` declaration: `width`,
`height`, `size`, `fill`, `offset` and `anchor` are read in device-independent pixels relative to the
native home area. There is no grid snapping and no free-spot search: tiles may start anywhere and cover
each other. A drag or resize on the phone saves a free pixel rectangle, so it wins over the file on the
next load. `~/.config/bellatrix/home/layout.toml` still holds the classic grid used by pinned apps and
folders; widget geometry is kept separately so agents and the grid do not fight over it.

```toml
version = 1
columns = 6   # the grid the cells are on; home.columns in ../style/style.toml sets the grid

[[page]]                  # the first home page; add more [[page]] blocks for more workspaces
tiles = [
  { widget = "50-memory.owl", x = 0, y = 0, w = 3, h = 4 },
  { app = "org.mozilla.firefox", x = 3, y = 0 },
  { folder = "Work", x = 4, y = 0, apps = ["com.slack", "linux:foot"] },
  { widget = "20-fetch.owl" },   # no x and y: the first free spot, at its size:
  { face = "morning.owl" },      # a face is a tile here, not another page
]
```

- `x` and `y` count cells from the top left for pinned apps, folders and the classic grid preview;
  widget tiles are no longer placed from these cells. A widget with no geometry in its root uses its
  declared `size:` as a starting size, near its `order:` position.
- `app` is an Android package (`"org.mozilla.firefox"`), an activity with its
  profile when there's more than one (`"com.android.chrome/.Main@10"`),
  `"linux:<desktop id>"`, or `"owl:<file>"` for an owl app. Adding one pins it;
  removing it takes it off home (it stays installed).
- A file left out of layout.toml stays where it is. A new `.owl` lands on the page its `page:` names
  (or page 1) by its `order:` and `size:`. To take a widget or face off home, move its file to `.removed/`.
- What couldn't be read is in `errors.txt` next to it, and on the phone's home.
  A file that doesn't parse changes nothing, and isn't saved over until it's fixed.

## Create and modify widgets on Bellatrix home

On the phone, long-press an empty part of home and use **new widget file**,
**edit home surface** or **edit taskbar surface**: they open those `.owl` files in Bellatrix's own
text editor, with no Linux or coding agent needed. Save, and the file appears on the real native
home within a second. Coding agents work too (the prompts point them at these same files), as does
any other writer of files.

Hold a widget tile and release without dragging to open Bellatrix's own native popup window: **remove**
moves the `.owl` file to `.removed/` and takes the tile off home, **edit** starts a Coding Agent
session for that file (or opens the standalone Coding Agents page when no agent is installed), and
**lock widget** pins its pixel rectangle so it cannot be moved or resized — the same item becomes
**unlock widget** immediately, while the menu is still open, and tapping it again switches straight back.
A locked tile still opens on tap. There is no always-visible dropdown button and no ring of handles.
A quick swipe never moves a tile: hold it first, then drag the body to move it. The bottom-right resize
area is invisible: pressing that corner starts resizing immediately and follows left/right/up/down in pixels,
with no hold and no drawn box. A locked tile has no such area. Tiles may overlap, and the home area is clipped without a visible border.
The empty home invite creates a widget or face tile directly when Linux isn't installed. There is no
second launcher screen — the files below are the ones Bellatrix home reads.

That is the Linux-style rice: each widget is a plain `.owl` file, not a database row or a Compose
theme. Write it with the native editor, a coding agent, `adb`, or anything else that can write a file,
save, and Bellatrix's one runtime redraws the tile. Use `owl-check <file>` for errors and a PNG made
with live data, and `owl-send <file> set <state> <value>` or `owl-send <file> refresh` to drive a live
tile. The shipped Bellatrix shell stays authoritative; only these files change how its native home and
taskbar look.

`home.owl` is an ordinary OWL `panel` drawn **over the Bellatrix desktop view you already see**, above
the taskbar and below app windows. Its tiles are clipped to that area, so they never escape the home
border. `taskbar.owl` does **not** create another taskbar: its compiled root `Box` properties
`background`, `radius`, `border`, `borderColor` and `padding` are applied to the real native
taskbar views; a visible `Flex`/`Row`/`Column` `gap` spaces task buttons, and a visible `Text` color
tints native icons. A transparent file restores Bellatrix's
shipped look. Native desktop icons, app windows, the start menu, search and task buttons stay in place.
They are seeded from Flex/Box/Anchor starters on the first install:

- `~/.config/bellatrix/widgets/home.owl` — the native home surface above the taskbar.
- `~/.config/bellatrix/widgets/taskbar.owl` — the native taskbar surface.
- `~/.config/bellatrix/widgets/widget-1.owl` — a new widget template using the same nodes on home.
- `~/.config/bellatrix/widgets/face-1.owl` — a face tile using the same nodes on home.

1. Write `~/.config/bellatrix/widgets/<name>.owl` (a `widget`, `face`, `app`,
   `panel`, or `keybar` declaration; see `README.md` and `docs/nodes.md`).
2. Run `owl-check ~/.config/bellatrix/widgets/<name>.owl`. It prints errors and
   the path of a PNG drawn with live data; open that PNG to check the layout.
   `home.owl` and `taskbar.owl` are previewed at the real desktop and taskbar
   sizes; a transparent surface gives a transparent PNG, which is how it leaves
   Bellatrix's native look visible. Add nodes or an opaque `background` if you
   want something to see in the preview.
3. Give it a starting rectangle with `width:`/`height:`/`size:`, `fill:`, `offset: [x, y]` and
   `anchor:` on its root `Box`/`Flex`/`Anchor`, or drag it on the phone. `layout.toml` keeps the
   classic grid for pinned apps and folders, not widget geometry.
4. `owl-send ~/.config/bellatrix/widgets/<name>.owl set <state> <value>` changes
   live state; `owl-send <file> refresh` redraws it.

Waybar-style declarative layout (owl 24):

- `Flex { … }` is a wrapping flex row: a surface module that flows to a new line
  when the view is narrow. It takes `gap`, `align`, `justify`, like `Row`.
- `Anchor { … }` pins children to its edges. Give each child an `anchor` of
  `topStart`, `top`, `topEnd`, `start`, `center`, `end`, `bottomStart`, `bottom`
  or `bottomEnd`. A full-size surface with a left clock and right icons is:

```owl
panel "home" {
  meta { owl: 24 }
  source now = clock every 1s
  source bat = android.battery
  Box {
    fill: true
    background: transparent           // change this to cover the native home surface
    Anchor {
      fill: true
      Text { anchor: topStart; text: fmtTime(now, "HH:mm") }
      Flex { anchor: topEnd; gap: 6
        Text { text: "${round(bat.level)}%" }
        Text { text: "󰀂" }
      }
    }
  }
}
```

Save that as `home.owl` and the modules sit on the native home surface at the size of the real
desktop area. `taskbar.owl` uses the same shape at the taskbar's size. `Box` accepts the same `anchor`
property, so an existing panel can be converted without changing its structure. The same root props
place a `widget` or `face` tile on home: `width: 320; height: 180; offset: [40, 24]` starts at 40×24 dp,
and `anchor: bottomEnd` pins it to the bottom-right corner. The shipped `widget-*.owl` and
`face-*.owl` files use `Box` around `Anchor`, so the same nodes can be checked as ordinary home tiles.

## Taskbar layout

`~/.config/bellatrix/taskbar/layout.toml` is the taskbar's pinned apps. Changes
made with the taskbar's pin/unpin menu write it; an agent editing it adds,
removes and reorders taskbar icons within a second. `errors.txt` next to it says
what was ignored. Transient state (which windows are open, minimized or focused)
is never written here — only pins.

```toml
version = 1
pins = ["UIExplorer", "UISettings", "UITerminal", "linux:foot", "org.mozilla.firefox"]
```

Names are the same keys home and the taskbar use: `UIExplorer`, `UISettings`,
`UITerminal`, `linux:<desktop id>`, or an Android package. A file that doesn't
parse changes nothing, and isn't overwritten until it's fixed.

Every screen has its own pages. A foldable's cover screen has its own grid and pages under `[cover]`: the same
tiles, placed for a screen a third as tall. The `[[page]]` blocks at the top say what's on home; `[[cover.page]]`
says which page each tile is on and where it sits there. A tile left out of `[cover]` keeps its main-screen page
and cell; without the section, the cover screen starts from the main pages with each tile at its own size. The
launcher writes the section the first time it's shown on the cover screen; deleting it starts over.

```toml
[cover]
columns = 6

[[cover.page]]              # a cover-screen page; add more for more workspaces
tiles = [
  { widget = "10-today.owl", x = 0, y = 0, w = 3, h = 3 },
  { widget = "20-fetch.owl", x = 3, y = 0, w = 3, h = 3 },
  { widget = "45-terminal.owl", x = 0, y = 3, w = 6, h = 3 },
]
```

The Fold's unfolded inner screen has its own `[unfolded]` columns and `[[unfolded.page]]` pages.
`home.unfolded-columns` in style.toml sets the wide grid, so tiles can span the hinge;
`home.unfolded-layout` is kept only so old files still read.

```toml
[unfolded]
columns = 12

[[unfolded.page]]
tiles = [
  { widget = "10-today.owl", x = 0, y = 0, w = 6, h = 3 },
  { widget = "45-terminal.owl", x = 6, y = 0, w = 6, h = 6 },
]
```

## From scripts

Anything in this system (a script, a cron job, a package hook, an agent) can reach a running widget:

```
$ owl-send build.owl set status passing     # sets the state `status`; a value that parses as JSON is JSON
$ owl-send build.owl set info - < info.json   # the value from stdin
$ owl-send build.owl refresh                  # runs its sources now
```

It waits for the launcher's answer and exits 1 if the widget isn't running or has no such
state. Setting a state works like a tap setting it: what reads it redraws, and triggers run.
For data a script writes anyway, a `file(...)` source is the other way: it updates the moment
the file changes.

```owl
widget "build" {
  meta { owl: 4 }
  state status = "unknown" persist
  source log = file("~/.cache/build.log")
  Column {
    Text { text: "build ${status}"; color: status == "passing" ? green : status == "failing" ? red : muted }
    Text { text: last(lines(log)) ?? "no log yet"; style: small; maxLines: 3 }
  }
}
```
