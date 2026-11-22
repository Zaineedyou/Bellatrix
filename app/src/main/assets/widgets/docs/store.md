# Widget store

Part of the widget docs: README.md, one folder up, comes first, and paths here are from the widgets folder.

The **widget store** installs reviewed widgets from https://github.com/SimonSchubert/omarchy-widgets. The store needs:

- `meta` with a `description` (10-200 characters), a `version` and a `license`, and ideally a `category`:
  `news`, `dev`, `productivity`, `time`, `world`, `sport`, `finance`, `media`, `games`, `pets`, `fun`, `home` or `system`
- every package in `requires`
- values in commands passed through `sh()` (see Values in commands in docs/declarations.md)
- nothing personal: no tokens, personal paths or hard-coded location
- a higher `version` for each update
- ideally a `preview.png` next to it: the one `owl-check` points to will do

The repo's CI checks the rules and summarizes every command for the reviewer. Store installs add a tile to Bellatrix's
one home page; `page:` and `order:` in the file only apply to widgets you add yourself.
