# Home and scripts

Part of the widget docs: README.md, one folder up, comes first, and paths here are from the widgets folder.

## Home's layout

Bellatrix home is hard-coded to one page. There is no face page, no empty feed page, and no second tile
page on a foldable. `face "name" { }` files are tiles on that same native grid, just like `widget` files.
A `page:` in any owl file is ignored; the tile still lands on the one home page.

`~/.config/bellatrix/home/layout.toml` holds that page's grid: where each tile sits, and which apps and
folders are pinned. Moving things on the phone rewrites it, and home follows your edits within a second.
A new file's `order:` and `size:` place it the first time; after that, move it here.

```toml
version = 1
columns = 6   # the grid the cells are on; home.columns in ../style/style.toml sets the grid

[[page]]                  # Bellatrix's only home page
tiles = [
  { widget = "50-memory.owl", x = 0, y = 0, w = 3, h = 4 },
  { app = "org.mozilla.firefox", x = 3, y = 0 },
  { folder = "Work", x = 4, y = 0, apps = ["com.slack", "linux:foot"] },
  { widget = "20-fetch.owl" },   # no x and y: the first free spot, at its size:
  { face = "morning.owl" },      # a face is a tile here, not another page
]
```

- `x` and `y` count cells from the top left; `w` and `h` are a widget's size.
  Apps, folders and faces are one cell unless they declare a size. A tile off the grid or on top of
  one before it takes the first free spot instead; if the page is full it takes the least crowded spot
  and is drawn above the others.
- `app` is an Android package (`"org.mozilla.firefox"`), an activity with its
  profile when there's more than one (`"com.android.chrome/.Main@10"`),
  `"linux:<desktop id>"`, or `"owl:<file>"` for an owl app. Adding one pins it;
  removing it takes it off home (it stays installed).
- A file left out of layout.toml stays where it is. A new `.owl` lands on the one page by its
  `order:` and `size:`. To take a widget or face off home, move its file to `.removed/`.
- What couldn't be read is in `errors.txt` next to it, and on the phone's home.
  A file that doesn't parse changes nothing, and isn't saved over until it's fixed.

## Create and modify widgets on Bellatrix home

On the phone, long-press an empty part of home and use **new widget file**,
**edit home bar** or **edit taskbar**: they open those `.owl` files in Bellatrix's own text editor,
with no Linux or coding agent needed. Save, and the file appears on home within a
second. Coding agents work too (the prompts point them at these same files), as
does any other writer of files. A widget tile's **edit file** action opens just that file, and the
empty home invite creates a widget or face tile directly when Linux isn't installed. There is no
second launcher screen — the files below are the ones Bellatrix home reads.

The two bar files are ordinary OWL `panel` files that render **inside the bar
Bellatrix already draws**, not swiped open as a panel and not a separate OWL home
screen. They are seeded from Flex/Box/Anchor starters on the first install, so they can be tested
without editing anything first:

- `~/.config/bellatrix/widgets/home.owl` — the strip above the prompt bar.
- `~/.config/bellatrix/widgets/taskbar.owl` — the strip below it, over the taskbar.
- `~/.config/bellatrix/widgets/widget-1.owl` — a new widget template using the same nodes on home.
- `~/.config/bellatrix/widgets/face-1.owl` — a face tile using the same nodes on home.

1. Write `~/.config/bellatrix/widgets/<name>.owl` (a `widget`, `face`, `app`,
   `panel`, or `keybar` declaration; see `README.md` and `docs/nodes.md`).
2. Run `owl-check ~/.config/bellatrix/widgets/<name>.owl`. It prints errors and
   the path of a PNG drawn with live data; open that PNG to check the layout.
3. Move it with `{ widget = "<name>.owl", x, y, w, h }` in
   `~/.config/bellatrix/home/layout.toml`, or let it take the first free spot.
4. `owl-send ~/.config/bellatrix/widgets/<name>.owl set <state> <value>` changes
   live state; `owl-send <file> refresh` redraws it.

Waybar-style declarative layout (owl 24):

- `Flex { … }` is a wrapping flex row: a status strip that flows to a new line
  when the tile is narrow. It takes `gap`, `align`, `justify`, like `Row`.
- `Anchor { … }` pins children to its edges. Give each child an `anchor` of
  `topStart`, `top`, `topEnd`, `start`, `center`, `end`, `bottomStart`, `bottom`
  or `bottomEnd`. A bar panel with a left clock and right icons is:

```owl
panel "home" {
  meta { owl: 24 }
  source now = clock every 1s
  source bat = android.battery
  Anchor {
    fill: width
    height: 40
    Text { anchor: topStart; text: fmtTime(now, "HH:mm") }
    Flex { anchor: topEnd; gap: 6
      Text { text: "${round(bat.level)}%" }
      Text { text: "󰀂" }
    }
  }
}
```

Save that as `home.owl` and the modules sit in Bellatrix's home bar. `taskbar.owl`
uses the same shape for the taskbar strip. `Box` accepts the same `anchor`
property, so an existing panel can be converted without changing its structure. The shipped
`widget-*.owl` and `face-*.owl` files use `Box` around `Anchor` too, so the same nodes can be checked
as ordinary home tiles.

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

Bellatrix home is one page on every screen. A foldable's cover screen has its own single grid under `[cover]`:
the same tiles, placed for a screen a third as tall. The `[[page]]` at the top says what's on home; `[cover]` only
says where those tiles go there. A tile left out of `[cover]` keeps its place; without the section, the cover screen
starts from the one home page with each tile at its own size. The launcher writes the section the first time it's
shown on the cover screen; deleting it starts over.

```toml
[cover]
columns = 6

[[cover.page]]              # the cover screen's one page
tiles = [
  { widget = "10-today.owl", x = 0, y = 0, w = 3, h = 3 },
  { widget = "20-fetch.owl", x = 3, y = 0, w = 3, h = 3 },
  { widget = "45-terminal.owl", x = 0, y = 3, w = 6, h = 3 },
]
```

The Fold's unfolded inner screen also has one page. Its `[unfolded]` section can only change that page's cell grid,
never add another page. `home.unfolded-columns` in style.toml sets that one wide grid, so tiles can span the hinge;
`home.unfolded-layout` is kept only so old files still read. The old two-page stories in older docs no longer apply.

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
