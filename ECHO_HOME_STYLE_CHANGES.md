# Home screen restyle — Echo Music card look

This build changes **only the cards in the home feed's horizontal shelf rows**
(the playlist cards and the song-suggestion cards) to match the grid card used
by the Echo Music app. Nothing else in the app was touched.

## What changed

| File | Change |
|---|---|
| `app/src/main/res/layout/item_shelf_lists_media.xml` | Rewritten as an Echo-Music-style grid card. |
| `app/src/main/res/layout/item_shelf_media_cover_big.xml` | Clean single square cover (stacked backdrop removed). |
| `app/src/main/res/drawable/rounded_rectangle_cover_echo.xml` | **New.** Cover shape with a 12 dp corner radius. |
| `app/src/main/res/values/themes.xml` | `itemCoverSize` 96 dp → **128 dp** (Echo Music's grid size). |
| `.../ui/feed/viewholders/MediaViewHolder.kt` | `applyCover()` — hides the stacked backdrop and forces a square cover. |

## The card, before → after

| | Before | After (Echo Music style) |
|---|---|---|
| Cover | stacked "list backdrop" behind playlist art | one clean square cover |
| Cover size | 96 dp / 128 dp (theme) | **128 dp** |
| Cover corner | 8 dp | **12 dp** |
| Cover shape | image aspect ratio | **square (1:1)** |
| Card padding | 4 dp | **12 dp** |
| Cover → title gap | 0 dp | **8 dp** |
| Title | default, up to 2 lines | **bold 16 sp, 1 line** |
| Subtitle | 12 sp, up to 2 lines | **14 sp, dimmed, up to 2 lines** |
| Alignment | centre-vertical | **left aligned** |

## Notes

- Both the **playlist** row and the **song-suggestion** row use
  `item_shelf_lists_media.xml`, so this one layout restyles both.
- The old `listBg1` / `listBg2` views are kept in the cover layout but are
  permanently hidden, so the generated view-binding still compiles. They can be
  deleted later (also remove the two `listBg1`/`listBg2` parameters of
  `applyCover()` and its four call sites).
- Because the layout is shared, the same card style also applies to shelf rows
  elsewhere in the app (artist/album/playlist pages, search) — not just the home
  screen. If you want the change to be home-only, say so and it can be scoped.

## Tuning

- **Card size** — `itemCoverSize` in `app/src/main/res/values/themes.xml`
  (128 dp now; try 144 dp for noticeably bigger cards).
- **Corner radius** — `app/src/main/res/drawable/rounded_rectangle_cover_echo.xml`.
- **Title / subtitle** — `item_shelf_lists_media.xml`.
