# Echo — home screen changes

Three things were changed in this build:

1. **Home shelf cards restyled** to the Echo Music grid-card look (playlist cards + song suggestions).
2. **A Material 3 hero carousel** added — the home feed's first shelf now renders as a hero carousel instead of a card row.
3. **A GitHub Actions workflow** that builds a debug APK *and* an unsigned release APK on every push.

---

## 1. Shelf card restyle

| File | Change |
|---|---|
| `app/src/main/res/layout/item_shelf_lists_media.xml` | Rewritten as an Echo-Music-style grid card. |
| `app/src/main/res/layout/item_shelf_media_cover_big.xml` | Clean single square cover (stacked backdrop removed). |
| `app/src/main/res/drawable/rounded_rectangle_cover_echo.xml` | **New.** Cover shape, 12 dp corner radius. |
| `app/src/main/res/values/themes.xml` | `itemCoverSize` 96 dp → **128 dp**. |
| `.../ui/feed/viewholders/MediaViewHolder.kt` | `applyCover()` — hides the stacked backdrop, forces a square cover. |

Cover is now one clean square (12 dp corners, 128 dp), 12 dp card padding, 8 dp gap,
bold 16 sp single-line title, dimmed 14 sp subtitle, left aligned.

## 2. Material 3 hero carousel

The home feed's **first shelf** is rendered as a Material 3 *hero* carousel
(`HeroCarouselStrategy`) instead of a horizontal card row. Everything else is unchanged.

| File | Change |
|---|---|
| `app/src/main/res/layout/item_shelf_hero.xml` | **New.** One carousel page: `MaskableFrameLayout` + cover + scrim + title/artist overlay. |
| `app/src/main/res/drawable/hero_scrim.xml` | **New.** Bottom-up dark gradient so the text stays readable. |
| `app/src/main/res/values/styles.xml` | **New** `ShapeAppearance.Echo.HeroCover` (24 dp corners). |
| `.../ui/feed/viewholders/shelf/ShelfViewHolder.kt` | **New** `ShelfViewHolder.Hero` holder. |
| `.../ui/feed/viewholders/HorizontalListViewHolder.kt` | Uses `CarouselLayoutManager(HeroCarouselStrategy())` + `CarouselSnapHelper` when the shelf is flagged as hero. |
| `.../ui/feed/FeedType.kt` | `HorizontalList` gains `hero`; `toFeedType()` gains `heroShelf`. |
| `.../ui/feed/FeedData.kt` | Passes `heroShelf` through to `toFeedType()`. |
| `.../ui/feed/FeedViewModel.kt` | `getFeedData()` gains `heroShelf`. |
| `.../ui/main/HomeFragment.kt` | Passes `heroShelf = true` (home feed only). |

### Tuning

| What | Where |
|---|---|
| Carousel height | `HERO_CAROUSEL_HEIGHT_DP` in `HorizontalListViewHolder.kt` (**360 dp**) |
| Card width | `android:layout_width` in `item_shelf_hero.xml` (**300 dp**) — must be a specific dp, not `wrap_content` |
| Corner radius | `ShapeAppearance.Echo.HeroCover` in `styles.xml` |
| Which shelf is the carousel | the `heroShelf` flag (`heroShelf && index == 0`) in `FeedType.toFeedType()` |

### Notes

- `CarouselLayoutManager` **requires** every item root to be a `MaskableFrameLayout`, and
  reads the **first item's width** to size the rest. Both are satisfied.
- Hero carousel items are masked as they scroll, so the peeking card's text is clipped —
  this matches the Material hero layout.
- The carousel only appears if the first shelf is a `Linear` list shelf with more than one item.

## 3. APK workflow

`.github/workflows/build-apks.yml` — on push to `main` (or manual run) it runs
`./gradlew assembleDebug assembleRelease` and uploads:

- `echo-debug-apk` → `app-debug.apk`
- `echo-release-unsigned-apk` → `app-release-unsigned.apk`

Download them from the workflow run's **Artifacts** section.
(The repo's existing `debug.yml`, `nightly.yml`, `stable.yml` and `pr.yml` are untouched.)

## 4. Thumbnail quality

Covers were being decoded from whatever small URL the extension supplied. Echo Music asks
Google's image service for a 544 px source (`resize(544, 544)`), so the same is now done here.

| File | Change |
|---|---|
| `.../utils/image/ImageUtils.kt` | New `upscaleThumbnail()` — rewrites the `=wN-hN` / `=sN` size token of Google / `ggpht` / `ytimg` image URLs to `w544-h544`. |
| `.../utils/image/ImageUtils.kt` | Disk cache key bumped (`IMAGE_CACHE_VERSION`) so previously cached low-res images are not reused. |

Applies to **every** network cover — playlist cards, the mini player, the big player, queue,
search, downloads — because they all go through the same request builder. Non-Google URLs and
URLs without a size token are left untouched.

Tune the resolution with `THUMBNAIL_SIZE` in `ImageUtils.kt` (544 dp, matching Echo Music).

## 5. Performance

| File | Change |
|---|---|
| `.../utils/image/ImageUtils.kt` | `loadInto()` now requests **hardware bitmaps**. Hardware bitmaps are much cheaper to draw, which removes most of the scroll/render jank. |
| `.../MainApplication.kt` | `getPackageName()` no longer walks the stack trace when called off the main thread (it was only meaningful there anyway). |
| `.../ui/feed/viewholders/MediaViewHolder.kt` | `applyCover()` only touches the layout params when the cover height actually changes. |

Hardware bitmaps are deliberately **not** enabled for the paths that read pixels back —
the blurred/gradient background and the circle/avatar transforms still get software bitmaps,
otherwise `toBitmap()` would throw.

---

## Build

Nothing new is required to build — the carousel uses the Material Components library the
project already depends on (`com.google.android.material:material`).

```
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # app-release-unsigned.apk
```
