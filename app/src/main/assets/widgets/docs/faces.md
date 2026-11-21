# Faces

Part of the widget docs: README.md, one folder up, comes first, and paths here are from the widgets folder.

Bellatrix home is hard-coded to one page. A `face "name" { }` file is a tile on that same native grid, like a
`widget` file — not a swipeable page and not a second screen. It uses the same declarations, nodes, `onTap`
and live sources as a widget. A `page:` in its file is ignored; `order:` and `size:` place it on the one
page the first time, and `~/.config/bellatrix/home/layout.toml` owns its cell after that. Faces need owl 10;
`Flex`, `Box`, `Anchor` and `anchor` need owl 24.

```owl
face "morning" {
  page: 1                 // ignored: Bellatrix has one home page
  order: 999
  size: 3x3               // cells on that page, columns x rows
  minSize: 2x2

  meta { owl: 24; description: "A clock face tile" }
  source now = clock every 1s

  Box {
    fill: width
    Anchor {
      fill: width
      Flex { anchor: topStart
        Text { text: fmtTime(now, "HH:mm"); style: huge; color: accent }
      }
      Flex { anchor: bottomEnd
        Text { text: fmtTime(now, "EEEE d MMMM"); style: muted }
      }
    }
  }
}
```

- **Where it goes.** Always on Bellatrix's one home page. It gets its declared `size:` (or a default
  3x3), and the launcher puts it in the first free spot. If the page is full it takes the least
  crowded spot and is drawn on top, so a new face is always visible; move or resize it on the phone
  afterwards, or in `~/.config/bellatrix/home/layout.toml`.
- **Native frame.** A face keeps Bellatrix's tile border, opacity and frosting like any other widget.
  The native home grid, recent-apps strip and bar stay in place. There is no face-only full-screen
  mode, no `statusBar: false` page and no empty feed page to swipe to.
- **Layout.** `Anchor { }` pins its children to edges with `anchor:`, `Flex { }` flows them into a row
  and `Box { }` gives them a frame. All three work in a face tile exactly as they do in `home.owl`
  and `taskbar.owl`; see docs/home.md for the bar examples.
- **Check it.** `owl-check <file>` prints compile errors and the path of a PNG of the tile with live
  data. Open that PNG to check the layout before saving to the phone.
- **Settings.** `home.empty-page-left`, `home.empty-page-right` and `home.home-page` are kept in
  style.toml only so old files still read; they no longer change home. Bellatrix always opens on its
  one page.
