# Mines: UI/UX Audit (Nov 2026)

Scope: repo HEAD `13c9803` (master, clean), Kotlin + Compose + Material 3, package `com.genoma.mines`. The evidence is static code reading only. No build was run and nothing was tested on a device. The baseline is the team's own `doc.md` (first audit sections 1-7 plus the "Second audit"). File paths below are relative to `C:\Users\hp\AndroidStudioProjects\Mines\`.

Severity scale: **Critical** (blocks play or breaks a platform rule), **High** (hurts the core loop or many users), **Medium** (polish or consistency), **Low** (nice to have).

Repo-evidence convention: claims marked `file:line` come from reading the code in this repo. That code is the primary source, so these claims have no URL. External claims carry a URL.

---

## 1. First-run and login gating: can players start immediately? Is there a guest flow?

### Takeaway
A guest flow exists ("Continue as guest", one tap to Home), but the app always cold-starts on the Login screen for guests, because only a signed-in profile is persisted. For signed-in players, hearts are a Firestore transaction, so they cannot start a game offline. Both add friction before the first tap on the board, while benchmark apps advertise no accounts and offline play.

### Cited Findings
- The start screen is Login: `var screen by rememberSaveable(...) { mutableStateOf<Screen>(Screen.Login) }` (`app/src/main/java/com/genoma/mines/MainActivity.kt:244-245`). The app skips past Login only `if (savedProfile != null && screen is Screen.Login)` (`MainActivity.kt:341-343`).
- `UserSessionStore` persists only USER_ID/NAME/EMAIL/PHOTO and has no guest flag (`feature/auth/src/main/java/com/genoma/mines/auth/data/UserSessionStore.kt:14-29`). Guest tap is `onGuestClick = { screen = Screen.Home }` (`MainActivity.kt:669-671`) and saves nothing. **So a guest sees the Login screen on every cold start (High).**
- Login copy: "Sign in with Google" (standard button, `heightIn(min = 48.dp)`), "Continue as guest" as a `TextButton`, and "Guest progress is saved on this device only." (`feature/auth/src/main/java/com/genoma/mines/auth/ui/LoginScreen.kt:138-170, 194`). Guest data (games, coins, diamonds, store items) is migrated on sign-in (`MainActivity.kt:584-640`). This is good practice.
- No SplashScreen API. A `sessionLoaded` flag gates the UI while DataStore loads (`MainActivity.kt:262, 345, 444`), and nothing in the repo matches `installSplashScreen`.
- Starting a game spends a heart first: `lives.consumeHeart()`. On an exception it shows "Couldn't start the game. Check your connection and try again." (`app/src/main/java/com/genoma/mines/presentation/viewmodel/MinesweeperViewModel.kt:345-361`). The signed-in heart store uses `firestore.runTransaction` (`feature/life/src/main/java/com/genoma/mines/life/data/remote/FirestoreLifeRepository.kt:48-59`). Firestore transactions need a server round-trip, so a **signed-in player offline cannot start a game (Critical for a single-player puzzle game)**. The heart refund on a win can also fail: "Couldn't return your heart. Check your connection." (`MinesweeperViewModel.kt:556-560`).
- Benchmarks: the open-source F-Droid Minesweeper (Joel Dankert) states it needs no network and has no ads, analytics, billing or accounts. It offers custom boards, no-guess mode and saved games ([F-Droid listing](https://f-droid.org/de/packages/com.joeld.minesweeper/)). The App Store app "Everyday Minesweeper" lists no ads, offline play, tap to reveal / long-press to flag, and local stats (per search summary of [mwm.ai listing](https://mwm.ai/apps/everyday-minesweeper/6784740045); not verified first-hand). Google Play "Minesweeper Classic" (Inspired Square) is described as offline-capable with custom boards ([AppGoblin](https://appgoblin.info/apps/com.inspiredsquare.minesweeper)).

### Inferences
- Recommended: persist "guest chosen" (or auto-enter as guest and move sign-in to Profile/Settings). Make the default first launch go straight to Home or an Easy board, with a one-time "save progress" nudge after the first win.
- Make hearts local-first (DataStore) with background sync for signed-in users, or let games start offline and reconcile later.

### Gaps
- The cold-start time and whether Login flashes before the redirect for signed-in users could not be measured (no device run).

---

## 2. Gameplay interaction (tap/long-press, chording, flag mode, zoom/pan, mis-tap protection, undo, pause, timer)

### Takeaway
The control basics are solid. Tap reveals, long-press always flags, a labelled Dig/Flag segmented switch outlines the board in red in flag mode, tapping a satisfied number chords, the first click is safe, and paused boards are covered. Several expected patterns are missing: zoom/pan, larger boards, undo or "continue after mine", question marks, a long-press duration setting, a timer that starts on first reveal, and saving a game in progress. The cell size (~38dp) is below the 48dp guideline.

### Cited Findings
- All three difficulties are 9x9: EASY 10, MEDIUM 20, HARD 30 mines (`feature/game/src/main/java/com/genoma/mines/game/domain/Difficulty.kt`). That is 37% density on Hard, which is mostly guessing. doc.md records "Grid sizes kept at 9×9 by decision". Antimine offers Beginner/Intermediate/Expert/Master/Legend/Custom ([Antimine README](https://github.com/lucasnlm/antimine-android)).
- Tap and long-press: `combinedClickable(onClickLabel = tapActionLabel, onClick = onTap, onLongClickLabel = "Toggle flag", onLongClick = onLongPress)` (`feature/game/src/main/java/com/genoma/mines/game/ui/GameScreen.kt:1111-1117`). In flag mode a tap routes to flag (`GameScreen.kt:264-271`).
- Chording is a tap on a revealed cell: `if (tappedCell.isRevealed) currentGame.chord(index)` (`MinesweeperViewModel.kt:458-462`). It is a no-op unless the flagged-neighbour count equals the number (`feature/game/src/main/java/com/genoma/mines/game/domain/MinesweeperGame.kt:212-227`). This matches desktop convention ([Minesweeper Blast mobile guide](https://minesweeperblast.pages.dev/minesweeper-mobile)). In flag mode, though, a tap on a number calls toggleFlag, not chord (`GameScreen.kt:264-269`), so chording is unavailable while flag mode is on (Medium).
- The first click is safe: mines are placed on the first reveal, excluding the cell and its neighbours (`MinesweeperGame.kt:15-45`). There is no no-guess generator. Antimine and the F-Droid Minesweeper both ship one ([Antimine README](https://github.com/lucasnlm/antimine-android); [F-Droid](https://f-droid.org/de/packages/com.joeld.minesweeper/)).
- Dig/Flag switch: `TapModeSegment` uses `selectable(role = Role.RadioButton)` with a `selectableGroup` (`GameScreen.kt:487-541`), and the board border goes to 2dp error colour in flag mode (`GameScreen.kt:941-960`). The mode is `remember { mutableStateOf(false) }` (`GameScreen.kt:153`), not saveable, so it resets on rotation (Low).
- Segment touch target: 16dp icon + `labelMedium` text with `padding(horizontal = 12.dp, vertical = 8.dp)` (`GameScreen.kt:541-548`) gives roughly 32-36dp height. That is below 48dp (Medium). See §6.
- No zoom/pan: `LazyVerticalGrid(columns = GridCells.Fixed(columns), userScrollEnabled = false, ...)` (`GameScreen.kt:963-966`). The board is sized `minOf(maxWidth, maxHeight)` (`GameScreen.kt:252`). Benchmarks with pinch-zoom/pan include Minesweeper Classic: Retro, which also has a zoom sensitivity setting, and Mineswifter, where reviewers ask for zoom that doesn't snap ([App Store, Mineswifter](https://apps.apple.com/us/app/id1521190195); [App Store id1287818410](https://apps.apple.com/gb/app/id1287818410?l=en-gb)). "Zoom out" is in Antimine's feature list ([Antimine README](https://github.com/lucasnlm/antimine-android)).
- Mis-tap protection: Leave and Restart mid-game ask for confirmation, with heart-cost copy (`GameScreen.kt:157-181, 356-388`). Cells are disabled unless PLAYING (`GameScreen.kt:260`). There is no confirm-on-reveal option, no adjustable long-press duration, and no question-mark state. Antimine offers "four different control styles", "custom long-press duration", optional question marks and an "adjustable touch area" ([Antimine README](https://github.com/lucasnlm/antimine-android)).
- Undo / hint / second chance: none. doc.md lists hint/undo as still open. Antimine has "continue after clicking a mine", "retry failed games" and an auto-flag assistant ([Antimine README](https://github.com/lucasnlm/antimine-android)).
- Pause: the bottom row has Pause/Play, Restart and How to Play (`GameScreen.kt:850-884`). `PausedBoardCover` hides the board and clears its semantics (`GameScreen.kt:971, 999-1053`). The game auto-pauses in `onStop` (`MainActivity.kt:183-187`).
- Timer: it starts in `beginGame()` before the first reveal (`MinesweeperViewModel.kt:364-385`), while classic Minesweeper starts on the first click (Low/Medium for speed-runners). The display clamps at 99:59 (`GameScreen.kt:562-567`). After 30 min of active play (`MAX_GAME_DURATION_SECONDS = 30 * 60`, `MinesweeperViewModel.kt:62`) the VM calls `goBackToHome()` with no message (`MinesweeperViewModel.kt:432-435`), and the heart is lost (Medium).
- Save/continue: none. The game lives only in the ViewModel, and doc.md lists "Continue game" as open. The F-Droid Minesweeper and Antimine both save and resume ([F-Droid](https://f-droid.org/de/packages/com.joeld.minesweeper/); [Antimine README](https://github.com/lucasnlm/antimine-android)).

### Inferences
- The largest gameplay gap against the market is board variety (larger and custom boards, no-guess) plus zoom/pan, which larger boards require.
- Starting the timer on the first reveal and letting chord work in flag mode are cheap fixes with visible fairness gains.

### Gaps
- Reliable Google Play review data for specific competitors (e.g. Antimine's or Minesweeper GO's rating and review themes) was not retrieved. The Antimine Play page returned 404 for the ID tried.

---

## 3. Hearts/lives gating: effect on the play loop and how it is communicated

### Takeaway
Every game costs a heart up front and a win refunds it, so only losses and abandons cost anything. There are 5 hearts with 1/hour regeneration and a refill at 5 diamonds per missing heart. Communication is good on Home and in the dialogs. But a skilled player who loses 5 times in a row waits up to 5 hours, this is unusual for a classic logic genre where competitors are free, unlimited and offline, and the gate also adds a network dependency.

### Cited Findings
- `LifeRules`: `MAX_HEARTS = 5`, `DIAMONDS_PER_HEART = 5`, `REGEN_INTERVAL_MILLIS = 60 min` (`feature/life/src/main/java/com/genoma/mines/life/domain/LifeRules.kt:5-19`). The comment still says "Gems (diamonds)" (`LifeRules.kt:8`).
- Spend up front, refund on win: documented at `MinesweeperViewModel.kt:337-344`, with refund at `:556-560`.
- Communication: the Start button shows "−1 ♥" and becomes "Out of hearts · Refill" at 0 (`feature/home/src/main/java/com/genoma/mines/home/ui/HomeScreen.kt:489-570`). "Play again −1♥" carries the a11y label "Play again, costs 1 heart" (`GameScreen.kt:683-702`). Leave/Restart dialog copy names the heart cost (`GameScreen.kt:357-367`). The hearts indicator has a countdown and a semantic description ("next heart in …") (`feature/life/src/main/java/com/genoma/mines/life/ui/HeartsIndicator.kt:106-130`). The hearts dialog shows the countdown, the diamond balance and a "Get diamonds" route to the Store (`feature/life/src/main/java/com/genoma/mines/life/ui/HeartsDialog.kt:49-153`).
- The Game screen itself shows no hearts count (the status bar is only MINES, Dig/Flag and TIME, `GameScreen.kt:431-473`), so the player learns they are out only after the game ends.
- Benchmarks: the leading free and open-source apps (Antimine, F-Droid Minesweeper) have no lives system ([Antimine README](https://github.com/lucasnlm/antimine-android); [F-Droid](https://f-droid.org/de/packages/com.joeld.minesweeper/)). The ad-supported "Minesweeper Classic" on Play is listed as 100% ad-monetised, 0% IAP ([AppGoblin](https://appgoblin.info/apps/com.inspiredsquare.minesweeper)).

### Inferences
- Consider: Easy always free (or unlimited practice mode), hearts only for ranked or Hard, a faster regen (e.g. 20-30 min), an optional rewarded-ad heart, and a "second chance" consumable that converts a loss into a continue instead of charging up front.
- Show remaining hearts on the result banner ("Play again −1♥ · 2 left").

### Gaps
- No primary data was found on player sentiment toward energy/lives systems in Minesweeper specifically, so retention impact can't be quantified. A sibling note (`market_popularity_monetization.md`) may cover monetisation.

---

## 4. Visual identity (ocean theme, backdrops, number colours, dark/light, store cosmetics)

### Takeaway
The identity is coherent and better than typical Minesweeper clones: teal brand, Deep Ocean variant, animated ambient scenes, hand-drawn flag/mine art, and two contrast-checked number palettes. The weak points are low hidden-vs-revealed tile contrast in light themes, ambient motion behind the board during play, the gold best-time chip, and a very thin cosmetic catalogue (4 items).

### Cited Findings
- Number colours: two palettes (light and dark cell), commented as "every pair ≥ 4.5:1 (WCAG AA)" (`core/theme/src/main/java/com/genoma/mines/core/theme/Color.kt:49-80`). The palette is picked from the luminance of the actual cell (`GameScreen.kt:1174-1177`). Digits are always drawn, so colour is not the only cue, which helps colour-blind players. Red 3 (`#B91C1C`) and green 2 (`#15803D`) are similar in lightness, but the digit disambiguates.
- Hidden vs revealed tile contrast, computed with the WCAG relative-luminance formula from the tokens: Classic light `#BEEAE4` vs `#FFFFFF` = **1.31:1**, Classic dark `#2C6259` vs `#17211F` = 2.35:1, Ocean light `#BEE4F5` vs `#FFFFFF` = 1.35:1, Ocean dark `#1B4B66` vs `#0D2530` = 1.70:1 (`Color.kt:20-38, 103-126`; cell colours at `GameScreen.kt:1074-1082`). All of these are below the 3:1 non-text contrast that WCAG 1.4.11 asks for UI component states. Hidden tiles rely on a 1dp border at 35% alpha (`GameScreen.kt:1095-1099`). **High** for the light themes, since the board is the main UI.
- White on `BestTimeGold #D4A017` = 2.38:1, used for the "Ns faster" chip in `labelSmall` (`GameScreen.kt:754-760, 787-791`). That fails the 4.5:1 that Android recommends for text under 18sp ([Android accessibility](https://developer.android.com/guide/topics/ui/accessibility/apps)). Medium.
- Dynamic colour is supported in `MinesTheme(dynamicColor = false)` but off, and there is no AMOLED/black theme (`core/theme/src/main/java/com/genoma/mines/core/theme/Theme.kt:95-113`). Antimine offers dynamic colours, AMOLED, multiple themes and skins ([Antimine README](https://github.com/lucasnlm/antimine-android)).
- The Game screen draws an animated scene behind the board: `GameSceneBackground` has `rememberInfiniteTransition` for rays shimmer and fish drift/wander, plus a frame loop (`feature/game/src/main/java/com/genoma/mines/game/ui/GameSceneBackground.kt:56, 195, 232, 308-315`). Classic uses `ClassicAmbientBackdrop(style = FOCUS)` (`GameScreen.kt:188-193`).
- Store catalogue: `classicTealTheme`, `deepOceanTheme`, `frostCellSkin`, `roboAvatar`, so 4 items total (`feature/store/src/main/java/com/genoma/mines/store/data/StoreCatalog.kt:16-21`).
- Immersive mode hides the status and nav bars on every screen, not only the game (`MainActivity.kt:151-161`).

### Inferences
- Raise hidden-tile contrast (a darker/saturated hidden fill, or a bevel/shadow) to at least 3:1 against revealed cells in light themes.
- Offer a "calm board" toggle that freezes the game-screen backdrop, independent of the system animation scale.
- Keep immersive mode for Game only. Hiding the clock and battery on Home, Store and Settings is unexpected.

### Gaps
- Contrast of `boardTheme.revealedCellColor`/`hiddenCellColor` in the Store theme objects (`DeepOceanTheme.kt`, `ClassicTealTheme.kt`) was not computed individually. The values above use the M3 scheme tokens.

---

## 5. Game feel (haptics, sound, animations, win/loss feedback, ReduceMotion)

### Takeaway
Feedback is deliberately layered. Distinct haptic waveforms and tone sequences play for tap, flag, explosion, win, best time, level-up and achievement, plus a cell colour cross-fade, confetti and a pulsing best-time chip. Sounds are synthesised `ToneGenerator` DTMF beeps on the music stream, which sound like a telephone, not a game. There is no reveal cascade or explosion animation. ReduceMotion covers only the system "Remove animations" switch.

### Cited Findings
- `GameFeedback` uses `ToneGenerator(AudioManager.STREAM_MUSIC, 80)` (`feature/game/src/main/java/com/genoma/mines/game/domain/GameFeedback.kt:33-36`). Win and level-up are DTMF digit runs (`:120-140, 196-210`). Haptics use `VibrationEffect.createPredefined(EFFECT_TICK/EFFECT_CLICK)` on API 29+ and waveforms for explosion and win (`:56-90, 104-109, 256-299`). Each event respects the Sound and Haptics toggles (`MinesweeperViewModel.kt:467-504, 535-538`).
- The tap haptic fires on every safe reveal, including chords and no-op taps that return null (`MinesweeperViewModel.kt:500-504`). A tap on an already-revealed number with unmet flags still ticks and beeps (Low).
- Animations: the cell background `animateColorAsState(tween(180))` (`GameScreen.kt:1087-1091`). Flood-fill reveal is instant, with no ripple cascade. The explosion is a static `BoardExplodedMine` drawing (`GameScreen.kt:1151-1155`). There is no board shake.
- Win: `ConfettiOverlay` behind the content (`GameScreen.kt:197-200`), the result card, and `TimeComparisonChip` with an infinite glow pulse (`GameScreen.kt:715-750`). Loss: "Game over · Hit a mine at mm:ss · N wrong flags", crossed-out wrong flags, and correct flags kept (`GameScreen.kt:305-329, 1068-1072, 1136-1149`). Celebration screen for achievements and level-ups (`feature/celebration/...`).
- `rememberReduceMotion()` returns `ANIMATOR_DURATION_SCALE == 0f`, inside `remember(context)`, so it is read once (`core/ui/src/main/java/com/genoma/mines/core/ui/components/ReduceMotion.kt:17-26`). It is used by the ocean and classic backdrops, confetti and the game scene frame loop (`AmbientOceanBackdrop.kt:64`, `ClassicAmbientBackdrop.kt:152`, `ConfettiOverlay.kt:121`, `GameSceneBackground.kt:308`). There is no in-app "reduce motion" setting (Settings has only Sound and Haptics, `feature/settings/src/main/java/com/genoma/mines/settings/ui/SettingsScreen.kt:235-275`).

### Inferences
- Swap `ToneGenerator` for short bundled SFX via `SoundPool` with `USAGE_GAME` audio attributes, add a staggered reveal cascade (gated by ReduceMotion), and a brief board shake on loss.
- Gate the tap feedback on whether the reveal or chord actually changed the board.

### Gaps
- Actual perceived quality of the sounds and haptics needs device testing.

---

## 6. Accessibility (TalkBack on cells, touch targets ≥48dp, WCAG 2.2 contrast, font scaling)

### Takeaway
TalkBack support on the board is real: each cell is announced as "Row r, column c, state" with custom click and long-click labels, and the paused board is hidden from screen readers. Touch targets on the board (~38dp) and on several custom controls (44dp pills, ~34dp Dig/Flag segments) are below Android's 48dp recommendation, although they pass WCAG 2.2's 24px minimum (SC 2.5.8). Tile-state contrast fails, and only one file uses string resources.

### Cited Findings
- Cell semantics: `.semantics { contentDescription = "Row $row, column $column, " + cellStateDescription(...) }` (`GameScreen.kt:1121-1124`). States include hidden, flagged, "N adjacent mines", "mine, exploded", "wrong flag, no mine here" and "flagged mine, correct" (`GameScreen.kt:1196-1205`). The number text is cleared from semantics to avoid double reading (`GameScreen.kt:1179`). The paused board uses `clearAndSetSemantics {}` (`GameScreen.kt:971`). Missing: a grid collection semantic (`collectionInfo`/`collectionItemInfo`), so TalkBack doesn't announce grid position natively, and there are no live-region announcements for the result or remaining mines.
- Board cell size: the 411dp-wide phone (doc.md's render width) minus 32dp screen padding, 12dp grid padding and 8×3dp gaps gives about 341/9 ≈ **37.9dp per cell** (`GameScreen.kt:123-128, 963-977`). On shorter screens the board shrinks further, because width is capped to the available height (`GameScreen.kt:252`). Android recommends at least 48dp×48dp touch targets ([Android: Make apps more accessible](https://developer.android.com/guide/topics/ui/accessibility/apps)). WCAG 2.2 SC 2.5.8 requires only 24×24 CSS px, or spacing ([W3C Understanding 2.5.8](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html)). So the board passes WCAG AA but not the Android guideline. That is partly inherent to a 9x9 grid on a phone, which is why zoom and adjustable touch area matter (Antimine lists "adjustable touch area" as an accessibility feature, [README](https://github.com/lucasnlm/antimine-android)).
- Home `IconPillButton` is `.size(44.dp)` for the Tournament/Settings pills (`HomeScreen.kt:577-586`), which is below 48dp. doc.md's "48dp touch target ✅" applies elsewhere (e.g. `LoginScreen.kt:194`, `HistoryScreen.kt:226` `minimumInteractiveComponentSize()`).
- GameTopBar uses M3 `IconButton` (48dp enforced) with 20dp icons (`GameScreen.kt:399-427`). This is fine.
- Text contrast: `onSurfaceVariant` on `surfaceVariant` is 4.83:1 in light and 8.17:1 in dark (pass). White on gold is 2.38:1 (fail, §4). Light-theme teal primary `#3FA9A0` on background `#F6F8F8` is 2.66:1 (fail if used for text or for meaningful icons, which need 3:1) (`Color.kt:7, 16`). Ratios computed from the token hex values with the WCAG formula. Thresholds per [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/apps) (4.5:1 for text under 18sp, 3:1 otherwise).
- Font scaling: a `fontScale = 1.5f` preview exists (`core/theme/src/main/java/com/genoma/mines/core/theme/MinesPreviews.kt:19`). Cell numbers use `(maxWidth * 0.55f).toSp()` (`GameScreen.kt:1131`), so they ignore the user's font scale. That is intentional for fit, but users who need larger numbers can't get them without zoom.
- Bottom nav: `NavigationBarItem` has no `label`, only the icon `contentDescription` (`core/ui/src/main/java/com/genoma/mines/core/ui/components/BottomNavbar.kt:28-51`). The Android Compose guide's standard example includes `label = { Text(destination.label) }`, and the bar is meant for "three to five destinations of equal importance" ([Android: Navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar)). One of the 4 slots is the placeholder More Games.

### Inferences
- Priority a11y fixes: hidden-tile contrast; 48dp for the Home pills and Dig/Flag segments; nav labels; a polite live region for "Game over" / "You won"; and grid collection semantics.
- Offer zoom or a "large cells" mode with a scrollable board. That resolves touch target and number legibility together.

### Gaps
- No real TalkBack or Accessibility Scanner run was possible. Focus order and swipe navigation across 81 cells are untested.

---

## 7. Edge-to-edge/insets, tablets/foldables/landscape, localisation

### Takeaway
Insets are handled ad hoc: only the top display cutout, per screen. Immersive mode hides the system bars app-wide. The app has zero adaptive layout code and doesn't declare itself a game. Since it targets SDK 37, Android ignores orientation and resizability limits on sw≥600dp devices, so the portrait-only column layout will be stretched or letterboxed on tablets and foldables. Landscape on phones squeezes the board to a strip. Localisation is not started.

### Cited Findings
- `WindowCompat.setDecorFitsSystemWindows(window, false)`, then hiding status and navigation bars with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, then `enableEdgeToEdge()` (`MainActivity.kt:151-163`). The app-level `Scaffold(contentWindowInsets = WindowInsets(0))` leaves each screen to handle the cutout (`MainActivity.kt:506-510`).
- The recent commit `13c9803` touched only `MainActivity.kt` (+5) and `GameScreen.kt` (+8), adding `windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))` (`GameScreen.kt:211-213`). The same pattern appears in `ScreenTopBar.kt:56` and `HomeScreen.kt:131-132`. Only Login uses `WindowInsets.safeDrawing` (`LoginScreen.kt:96`). In landscape the cutout is on a side edge and is not padded (Medium). When the user swipes the transient bars in, they overlay content, which is acceptable for a game, but the bottom `NavigationBar` and the Game's bottom actions don't account for `systemGestures` insets.
- Edge-to-edge can't be opted out of when targeting API 36+ ([Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)). The app already complies.
- `targetSdk = 37`, `minSdk = 24` (`app/build.gradle.kts:15-16`). For apps targeting API 36+, "orientation, resizability, and aspect ratio restrictions no longer apply on displays with smallest width >= 600dp". The temporary opt-out "won't apply when targeting API level 37". **Games are exempt via `android:appCategory`** ([Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)). The manifest has no `android:appCategory="game"`, no `screenOrientation` and no resizability attributes (`app/src/main/AndroidManifest.xml:8-27`).
- The repo has no `WindowSizeClass`, `currentWindowAdaptiveInfo` or orientation handling (no grep matches). Google defines compact width <600dp, medium 600-840dp, expanded 840-1200dp and compact height <480dp ("99.78% of phones in landscape"), and recommends `currentWindowAdaptiveInfo().windowSizeClass` ([Android: window size classes](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)).
- Landscape phone estimate: about 411dp of height minus the top bar (48), two 20dp spacers, the status bar (~70), the 14dp spacer, the bottom actions (~70) and 16dp padding leaves roughly 150dp for the board, so cells of ~14dp (`GameScreen.kt:208-339`). This is an estimate from the layout code, not a render. **High**: the layout is unplayable in landscape and needs a side-by-side layout (board left, controls right) for compact height.
- Rotation state: the screen is `rememberSaveable` (`MainActivity.kt:244`), and the game lives in the ViewModel (survives rotation but not process death). Flag mode is lost on rotation (`GameScreen.kt:153`).
- Predictive back: apps targeting API 36+ get predictive back animations by default, and `onBackPressed` is not called ([Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)). The app uses Compose `BackHandler` (`MainActivity.kt:461-503`, `GameScreen.kt:167`), which is the supported API.
- Localisation: `app/src/main/res/values/strings.xml` is 4 lines, and `stringResource` appears only in `MainActivity.kt`. All UI copy is hard-coded (e.g. `GameScreen.kt:293-323, 357-367`). Pluralisation is hand-rolled (`GameScreen.kt:318-321`; `MinesweeperViewModel.kt:799, 813`). The time format uses `"%02d:%02d".format` with no locale (`GameScreen.kt:566`). `android:supportsRtl="true"` is set (`AndroidManifest.xml:15`), and arrows use `AutoMirrored` (`GameScreen.kt:63`), so RTL is half-ready.

### Inferences
- Short-term: add `android:appCategory="game"` (this keeps any orientation choice honoured on large screens) and a compact-height landscape layout. Medium-term: a two-pane layout on expanded widths (board plus stats/history), with the cell size capped so a 9x9 board doesn't become 90dp tiles on tablets.
- Replace the per-screen `displayCutout.only(Top)` with `safeDrawing` / `safeContent` on all sides for each screen root.

### Gaps
- Foldable hinge (posture) behaviour and multi-window weren't testable.

---

## 8. Status of every doc.md item (verified against current code)

### Takeaway
Most doc.md fixes are present in the code. The carry-overs are confirmed still open: localisation, hint/undo, continue game, wallet UI / daily reward / onboarding, real difficulty sizes and zoom. Two items marked fixed are only partial: 48dp touch targets and the TalkBack board (labels yes, grid semantics and live regions no).

### Cited Findings (repo evidence)
| doc.md item | Status | Evidence |
|---|---|---|
| §1 Game Settings/How-to-Play dead | Fixed | `GameScreen.kt:144-145` callbacks; bottom row `:877-882` |
| §1 Back on Tournament/Celebration | Fixed | `MainActivity.kt:475-481` |
| §1 Confirm leaving game | Fixed | `GameScreen.kt:157-181` |
| §1 Restart silently costs heart | Fixed | Restart dialog `:363-367`; "Play again −1♥" `:683-702` |
| §1 Nav state in `remember` | Fixed (screen) / Partial (flag mode `GameScreen.kt:153` not saveable; game lost on process death) | `MainActivity.kt:244` |
| §2 Board visible while paused | Fixed | `GameScreen.kt:999-1053` |
| §2 Auto-pause on background | Fixed | `MainActivity.kt:183-187` |
| §2 All difficulties 9×9 / zoom | Open (by decision) | `Difficulty.kt`; `GameScreen.kt:965` |
| §2 Flag mode not obvious | Fixed | `GameScreen.kt:481-560, 941-960` |
| §2 Flag haptic | Fixed | `MinesweeperViewModel.kt:535-538` |
| §2 Cell icons fixed 16dp | Fixed | `GameScreen.kt:1129-1131` |
| §2 Loss banner thin | Fixed | `GameScreen.kt:305-329` |
| §2 Hint/undo | Open | no code |
| §3 Unlabelled home pills | Fixed (labels) / Partial (44dp size) | `HomeScreen.kt:309, 316, 585` |
| §3 Duplicate trophy icons | Partial: `EmojiEvents` still used for Achievements nav (`BottomNavbar.kt`) and the win badge (`GameScreen.kt:619`) | |
| §3 Hero 220dp | Fixed per doc.md (not re-verified at 360dp) | |
| §3 Start shows heart cost / 0 hearts | Fixed | `HomeScreen.kt:489-570` |
| §3 Difficulty cards best time | Fixed | `HomeScreen.kt:794-839` |
| §3 Continue game | Open | |
| §4 More Games placeholder in nav | Partial: improved page, still a nav slot | `BottomNavbar.kt` `MORE_GAMES` |
| §4 Tournament placeholder | Partial: copy fixed, practice button, still no tournament | doc.md 2nd audit #13 |
| §4 Wallet UI | Open | `feature/wallet` has data only |
| §4 Daily reward/streak/onboarding | Open | no code found |
| §5 Hard-coded colours | Mostly fixed (tokens in `Color.kt`); a few remain e.g. `DetonatedCellRed` `GameScreen.kt:121`, `Color.White` on gold `:589, 757` | |
| §5 Mixed button styles | Partial: Result uses M3 `Button`; `GameActionButton`, `TapModeSegment`, `IconPillButton` are still hand-rolled with roles | `GameScreen.kt:886-922` |
| §5 Ocean theme only on Game | Fixed | backdrops in `core/ui` |
| §5 Previews | Fixed | `MinesPreviews.kt` (incl. fontScale 1.5) |
| §6 contentDescription nulls | Fixed for interactive items | |
| §6 Board semantics | Partial: labels and actions yes; no collection semantics or live region | `GameScreen.kt:1111-1124` |
| §6 Number colour contrast | Fixed (numbers) / Open (tile-state contrast 1.3:1) | `Color.kt:49-80` |
| §6 Small text / 1.3× | Fixed per doc.md | |
| §6 48dp touch target | Partial: cells ~38dp, pills 44dp, segments ~34dp | §6 above |
| §7 Localisation | Open | `strings.xml` 4 lines |
| 2nd audit #1-#18 | Fixed per doc.md. Spot-checked #1 (banner below board, `GameScreen.kt:286-339`), #2 (confetti behind, `:195-200`), #11 ("diamonds" in `HeartsDialog.kt:96`; but `LifeRules.kt:8` comment still says "Gems"), #12 (Login, `LoginScreen.kt:138-170`) | |

### Inferences
- doc.md's "all fixed" claims are accurate for the visible bugs. The remaining debt is structural: board size and zoom, offline hearts, adaptive layout and localisation.

### Gaps
- 2nd-audit items #3-#10 and #13-#18 were not each re-verified line by line. They are taken from doc.md plus spot checks.

---

## 9. Scorecard (1-10) and missing UX patterns

### Takeaway
This is a polished-looking, well-intentioned app whose core loop is held back by product decisions (9x9 only, the hearts gate, login on every launch, network dependency) more than by craft.

### Cited Findings / Scores (rationale from sections above)
| Area | Score | Why |
|---|---|---|
| Onboarding / first run | 5 | Guest path exists and migrates data; but Login on every cold start, no splash, no tutorial board (§1) |
| Core controls | 7 | Tap/long-press, Dig/Flag switch, chord, safe first click, confirm dialogs; no zoom, no long-press setting, no chord in flag mode (§2) |
| Game depth / modes | 3 | Three 9x9 presets, no custom or no-guess, no daily puzzle (§2) |
| Hearts / economy UX | 5 | Clearly communicated; harsh 1h regen; needs network (§3) |
| Visual identity | 7 | Cohesive ocean/teal art, contrast-checked numbers; weak tile contrast, 4 cosmetics (§4) |
| Game feel | 6 | Rich haptics, confetti, best-time chip; beepy DTMF audio, no reveal cascade or explosion animation (§5) |
| Accessibility | 6 | Real TalkBack cell labels; sub-48dp targets, tile contrast, no nav labels (§6) |
| Adaptive / insets | 3 | Top-cutout only; no landscape/tablet layouts; target 37 without `appCategory=game` (§7) |
| Localisation | 1 | Hard-coded strings (§7) |
| Navigation / IA | 6 | Shared top bar and back fixed; placeholder tab in nav, icon-only nav (§6, §8) |

Missing UX patterns players expect, with benchmark sources:
- Larger and custom boards (e.g. 16×16, 16×30, custom) — [Antimine](https://github.com/lucasnlm/antimine-android), [F-Droid Minesweeper](https://f-droid.org/de/packages/com.joeld.minesweeper/)
- No-guess board generation — same sources
- Pinch-zoom/pan and adjustable touch area or zoom sensitivity — [Antimine](https://github.com/lucasnlm/antimine-android), [App Store id1287818410](https://apps.apple.com/gb/app/id1287818410?l=en-gb), [Mineswifter](https://apps.apple.com/us/app/id1521190195)
- Save and resume a game in progress — [F-Droid](https://f-droid.org/de/packages/com.joeld.minesweeper/), [Antimine](https://github.com/lucasnlm/antimine-android)
- Continue after hitting a mine / retry the same board — [Antimine](https://github.com/lucasnlm/antimine-android)
- Control-style choices, custom long-press duration, optional question marks — [Antimine](https://github.com/lucasnlm/antimine-android)
- Fully offline play, no account — [F-Droid](https://f-droid.org/de/packages/com.joeld.minesweeper/)
- AMOLED and dynamic colour themes — [Antimine](https://github.com/lucasnlm/antimine-android)
- Statistics (win rate, streaks per difficulty): Antimine lists "game statistics" ([README](https://github.com/lucasnlm/antimine-android)); Mines has History/Profile stats (partly present)
- Not sourced to competitors, but standard for the genre: timer starting on first click, a daily challenge, and a 3BV/efficiency stat.

### Inferences
- Suggested priority: (1) offline-first hearts plus remember-guest. (2) Hidden-tile contrast and 48dp targets for pills/segments. (3) `appCategory="game"` plus a compact-height landscape layout. (4) Bigger boards with zoom/pan and no-guess. (5) Save/continue plus undo or second chance as a diamond consumable. (6) Strings extraction. (7) A SoundPool SFX pass and reveal cascade.

### Gaps
- No Google Play ratings or review-text analysis for top Minesweeper apps was obtained (the Play page fetch failed). The benchmark relies on README and store-description feature lists, which say what apps offer, not what players value most.
