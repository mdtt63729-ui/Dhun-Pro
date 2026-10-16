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

## 6. Build fixes

The first CI run failed to compile; these three fixes are applied:

| Error | Fix |
|---|---|
| `getFeedData(...)` callers passing a `Flow` positionally (`DownloadFragment`, `SearchFragment`, `MediaDetailsFragment`) | Moved the new `heroShelf` parameter **after** the `vararg extraLoadFlow`, so existing positional calls still bind correctly. |
| `HorizontalListViewHolder.kt` — unresolved `listener` in `bind()` | `listener` is now a `private val` constructor property (a plain constructor parameter is only visible in initializers). |
| `ImageUtils.kt` — unresolved `allowHardware` | Added the missing `import coil3.request.allowHardware` (`ImageRequest.Builder.allowHardware` is an extension function in Coil 3). |

## 7. Thumbnail quality, round two

The first pass capped covers at 544 px, which is fine for a 128 dp card but visibly soft on the
full-screen player (a ~350 dp cover is ~1050 px on a 3x screen).

| Change | Detail |
|---|---|
| Resolution | `THUMBNAIL_SIZE` 544 → **1000 px** in `ImageUtils.kt` — well above Echo Music's `resize(544, 544)`. Coil still downsamples to whatever the view actually needs. |
| YouTube stills | URLs with no `=` size token (e.g. `/vi/<id>/mqdefault.jpg`) are upgraded to `hqdefault.jpg`. |
| Cache | Disk cache key version bumped so the old low-res copies are not reused. |

This applies to every network cover — playlist cards, the songs listed inside a playlist, the
mini player and the full-screen player — because they all share one request builder.

## 8. Home carousel behaviour

| Change | Where |
|---|---|
| The home feed's first shelf is still the hero carousel and always stays on top. | `FeedType.toFeedType()` |
| Its contents are **shuffled with a per-load seed** and capped at 15 items, so it is different on every refresh. | `FeedType.heroItems()` |
| Items are stably biased **playlists → albums → artists → songs**, so playlists dominate. | `FeedType.heroPriority()` |
| The shelves **below** the carousel are shuffled on every load too, with playlist shelves biased towards the top. | `FeedData.shuffleBelowFirst()` |
| The seed is regenerated on every load/refresh. | `FeedData.heroSeed` |
| Only the first shelf of the first page becomes the carousel (paged feeds no longer produce several). | `FeedData` `firstPage` guard |

Tuning: `HERO_ITEM_LIMIT` in `FeedType.kt` (15).

## 9. Splash screen

The splash is the Android 12+ system splash (`art_splash_anim`, 900 ms) and nothing held it on
screen, so the home appeared as soon as the first frame was drawn — cutting the animation off.

| File | Change |
|---|---|
| `MainActivity.kt` | `installSplashScreen()` + `setKeepOnScreenCondition { !splashFinished }`, released after `SPLASH_ANIM_DURATION_MS` (900 ms). |
| `values/themes.xml` | Added the `core-splashscreen` equivalents (`windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon`, `windowSplashScreenAnimationDuration`, `postSplashScreenTheme`) so the splash also renders on API < 31. |

## 10. Dynamic Color (whole-UI tint from the song artwork)

The app already had the mechanism — `player_app_color` (`PLAYER_COLOR`) feeds the current
track's artwork accent into `DynamicColors.setContentBasedSource` — but there was **no switch
for it** and it was only applied once, when the activity was created.

| File | Change |
|---|---|
| `ui/settings/SettingsLookFragment.kt` | New **Dynamic Color** switch (key `player_app_color`, default off) under *Colours*. |
| `MainActivity.kt` | `observeDynamicColor()` — rebuilds the theme when the artwork accent changes enough to notice. |
| `res/values/strings.xml` | `dynamic_color`, `dynamic_color_summary`. |

Guards, so it can't thrash: the theme is rebuilt only when the hue moves more than
`THEME_HUE_THRESHOLD` (24°) or saturation by 25%, at most once every
`THEME_RECREATE_INTERVAL_MS` (8 s), and only while the UI is `RESUMED`. The applied colour is
kept in a static so a recreate can't loop.

*(Echo Music / Muso do this in Compose, where the theme simply recomposes — `Bitmap.extractThemeColor()`
+ `Palette` + `Score`. In this XML app the only way to rebuild the theme is to recreate the
activity, hence the guards.)*

## 11. Settings toggling crash / lag — fixed

Three separate causes:

| Cause | Fix |
|---|---|
| **The splash was held on every recreate.** The 900 ms keep-on-screen condition ran on config changes too, so toggling a theme setting recreated the activity and showed the splash again — looking exactly like the app closing and restarting. | `MainActivity` now holds the splash only on a cold start (`savedInstanceState == null`). |
| `requireActivity().recreate()` called **inside** the `OnSharedPreferenceChangeListener`, with no guard — a detached fragment threw, and two quick toggles queued two recreates. | `recreateSafely()`: deferred to `view.post`, one at a time, and skipped if the activity is finishing/destroyed. |
| The Back-Animations switch changed the **enabled activity component** synchronously from the same callback, which can take the running activity down. | Also deferred via `view.post` with the same guards. |

---

## Build

Nothing new is required to build — the carousel uses the Material Components library the
project already depends on (`com.google.android.material:material`).

```
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # app-release-unsigned.apk
```
