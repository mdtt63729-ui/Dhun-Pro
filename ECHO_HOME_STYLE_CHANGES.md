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

## 12. Hero carousel is now "live" and uncapped

| Change | Where |
|---|---|
| The carousel **advances on its own** every `HERO_AUTO_ADVANCE_MS` (5 s), looping forever. | `HorizontalListViewHolder` — a `Runnable` posted on the RecyclerView; started/stopped on attach/detach and when the hero mode changes. |
| **No item cap** — every item the extension sent goes into the carousel (`HERO_ITEM_LIMIT` removed). | `FeedType.heroItems()` |

## 13. Home scroll bar removed

The fast-scroll bar sat on top of the hero carousel. `MainFragment.applyInsets()` gained a
`scrollBar` flag and `HomeFragment` passes `false`, so the home feed has no scroll bar at all
(the `Scroll Bar` setting still controls it everywhere else).

## 14. Dynamic Color is smooth again

Rebuilding the theme means recreating the activity, which flashed and stuttered on **every**
track change. The live rebuild is gone: `Dynamic Color` now tints the app from the current
song's artwork **at activity creation** (i.e. it follows the song from the next app start),
so nothing flashes or lags while you listen. The `PLAYER_COLOR` wiring in `applyUiChanges`
is unchanged — only the auto-recreate was removed.

## 15. Settings list no longer jumps to the top

Toggling a theme/colour setting recreates the activity, and the preference list came back at
the top. `SettingsLookFragment` now records the first visible position before the recreate
(`pendingScroll`) and restores it in `onResume`.

## 16. Mini player → full player is instant now

`GestureListener.handleGestures()` fired the click from `onSingleTapConfirmed`, which the
framework only delivers **after the double-tap timeout (~300 ms)**. So every tap on the mini
player sat idle for 300 ms before the full player started opening.

Now `onSingleTapUp` acts immediately whenever there is no double-tap action to wait for
(which is the case while collapsed), remembers that it did, and swallows the later
`onSingleTapConfirmed`. A second tap inside the double-tap window is ignored, so a double tap
still behaves like it used to. When a double-tap action *is* set, nothing changes.

## 17. Player cover no longer goes soft when playback starts

The player cover was loaded from `item.track.cover` — the **loaded** track's artwork, which the
extension usually returns as a much smaller thumbnail than the one the feed gave us. So the
artwork was crisp until the track loaded and then dropped in quality.

`PlayerTrackAdapter` now prefers `item.unloadedCover` (the feed artwork, which was already
being shown as the transition thumbnail) and only falls back to `track.cover`. This covers both
the mini player and the full-screen player, since they share the same view holder.

## 18. Accent colour cycling / flash / lag during playback — fixed

`PlayerFragment.configureColors()` recreated the activity **on every track change** whenever
`PLAYER_COLOR` was on:

```kotlin
if (context.isPlayerColor() && context.isDynamic()) {
    if (uiViewModel.currentAppColor != ...track?.id) {
        requireActivity().recreate()   // <-- here
    }
}
```

Every recreate rebuilt the theme, so the whole UI's accent colour visibly cycled, flashed and
stuttered mid-playback. The recreate is gone — the whole-app tint is applied once, when the
activity is created (`MainActivity.applyUiChanges`), which is also how the Dynamic Color switch
behaves.

## 19. Settings list jumping to the top — properly fixed

`BaseSettingsFragment.onViewCreated()` called `replace(...)` on the child fragment container
every single time. On a recreate the FragmentManager has *already* restored that child —
replacing it again threw its saved state (including the preference list's scroll position)
away. That is why toggling a theme setting made the list jump to the top, and why the earlier
`pendingScroll` workaround did not stick.

The child is now only created when it is not already there.

## 20. Loaded-track artwork was still soft — fixed

`upscaleThumbnail()` only rewrote a size token when one was **already present**. The loaded
track's artwork — the one the player endpoint returns, and the one every "now playing" cover
in the app uses (mini player, full-screen player, queue, notification) — is usually a plain
URL with **no token at all**, so it was left untouched and decoded small.

Now, for a resizable host (`googleusercontent.com`, `ggpht.com`, `ytimg.com`):

1. a size token, if present, is rewritten to `w1000-h1000`;
2. a small YouTube still (`default` / `mqdefault`) is swapped for `sddefault`;
3. otherwise — when the path is a bare id with no other `=` parameter — `=w1000-h1000-l90-rj`
   is **appended** (any query string is preserved).

The disk cache key version went to 3 so previously cached small covers are re-fetched.

## 21. Theme colour now follows the song — smoothly, with no visible rebuild

In an XML/View app a theme change means **recreating the activity**; there is no re-compose.
That is the only way to re-tint the whole UI, and doing it while the user is looking at the
screen is exactly the flash, the stutter and the content reloading (the screen going blank
for a moment) that the screen recording shows.

The rebuild is therefore deferred until it cannot be seen. `MainActivity` now:

- remembers the artwork colour the on-screen theme was built from (`appliedAccent`);
- watches the player's colours while the app is visible and, when the song's accent has moved
  far enough to matter (`THEME_HUE_THRESHOLD`), just sets a flag — it does **not** rebuild;
- rebuilds only once the app has been off-screen for `RE_THEME_DELAY_MS` (1.5 s), and cancels
  that if the user comes back first.

So the whole-app colour still follows the song, but the rebuild happens in the background and
the user never sees it — no lag, no loading, no colour cycling.

## 22. Correction: the re-theme is on screen again — but it no longer blanks the screen

Deferring the rebuild to the background (section 21) made the colour stop following the song
while you were using the app. That was wrong. The rebuild is back on screen, and what actually
made it look bad is fixed instead:

- `AnimationUtils.setupTransition()` returned early when `activity.isChangingConfigurations`
  is true — which is exactly the flag `recreate()` sets. The `MaterialSharedAxis` enter
  transition was what made the content flash blank and slide back in on every colour change.
  With no transition the new theme is simply there.
- The rebuild is debounced by `RE_THEME_DELAY_MS` (700 ms) so skipping through tracks does not
  thrash it, and it is skipped while one is already in flight.

So the whole UI now follows the song's wallpaper, live, with no blank frame and no slide-in.

## 23. Loading indicators are now Material 3 everywhere

Every indeterminate spinner now uses Material's expressive
`com.google.android.material.loadingindicator.LoadingIndicator` (the shape-morphing one), and
none of them draw the solid container circle — `app:containerColor="@android:color/transparent"`
— so the white disc in the screenshot is gone:

| Layout | Was | Now |
|---|---|---|
| `item_loading.xml` | `LoadingIndicator` with the default container | container transparent |
| `item_loading_small.xml` | container `?colorPrimaryInverse` | container transparent |
| `dialog_extensions_list.xml` | `CircularProgressIndicator` | `LoadingIndicator` |
| `item_player_controls.xml` | `CircularProgressIndicator` | `LoadingIndicator` |
| `item_player_collapsed_controls.xml` | `CircularProgressIndicator` | `LoadingIndicator` |
| `item_playlist_track.xml` | `CircularProgressIndicator` | `LoadingIndicator` |

`item_download_task.xml` keeps its `CircularProgressIndicator` on purpose — that one is a
*determinate* ring showing download progress, which the loading indicator cannot express.

## 24. Playlist / media header restyled after Muso

The header (`item_media_header.xml`) now opens with a compact square cover on the left and the
title block beside it — bold title plus a one-line type/song-count summary — and, when there
are exactly two actions, they become full-width side-by-side buttons (filled first, outlined
second), matching Muso's Shuffle/Radio pair. `MediaHeaderAdapter` binds the cover, title and
subtitle, and `configureButtons()` gives the pair equal weights.

## 25. The big collapsing cover is gone — the playlist screen really is Muso-shaped now

Section 24 only restyled the header, but the screen was still dominated by the collapsing
artwork in `fragment_media.xml`, so it still looked like before. That is removed:

- `fragment_media.xml` is now a plain `MaterialToolbar` in an `AppBarLayout` — no
  `CollapsingToolbarLayout`, no big cover, no `endIcon`.
- `MediaFragment` no longer touches `coverContainer` / `cover` / `endIcon`; the cover, title
  and summary come from the scrolling header instead.
- `MediaHeaderAdapter.configureButtons()` dropped the old `-56dp` negative margin (it existed
  only to tuck the buttons under the collapsing cover) and the description's trailing margin.

The screen now opens straight onto the header: compact cover, title, summary, the action row,
and the track list.

## 26. Loading indicator disc, properly this time — and the player's scroll bar

The white circle was still showing even though each layout set `containerColor` to transparent,
so it is now forced at the theme level instead — `loadingIndicatorStyle` is overridden with
`EchoLoadingIndicator` (parent `Widget.Material3.LoadingIndicator`) whose `containerColor` is
transparent. That covers every `LoadingIndicator` in the app, wherever it is inflated from.

The full-screen player had a scroll bar down its right edge. `fragment_player.xml` (the
`ViewPager2`), `fragment_player_queue.xml` and both lists in `fragment_player_lyrics.xml` now
carry `android:scrollbars="none"`.

---

## Build

Nothing new is required to build — the carousel uses the Material Components library the
project already depends on (`com.google.android.material:material`).

```
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # app-release-unsigned.apk
```
