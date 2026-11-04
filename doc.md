# Mines — UI/UX & Feature Shortcomings

Quick review from the Compose `@Preview`s (14 screens/components) plus the wiring in `MainActivity.kt` and `MinesweeperViewModel.kt`. Ordered by impact.

---

## 1. Broken or dead interactions (fix first)

| Issue | Where | Why it matters |
|---|---|---|
| **Settings and How to Play buttons on the Game screen do nothing.** `GameScreen` defaults `onSettings = {}` and `onHowToPlay = {}`, and `MainActivity` never passes them. | `MainActivity.kt` ~L825–870, `GameScreen.kt:118-119` | Two of the six visible buttons on the main screen do nothing when tapped. Players will think the app is broken. |
| **System back does nothing on Tournament (and Celebration).** `BackHandler` is enabled for every screen except Home/Login, but Tournament and Celebration fall into the empty `else` branch. | `MainActivity.kt:374-406` | The player gets stuck and can only leave through the UI. |
| **Back from a game exits immediately, with no confirmation.** A heart has already been spent, so one accidental back press loses it. | `MainActivity.kt:379` | Frustrating and feels like a punishment. Show an "Abandon game? You'll lose this heart" dialog. |
| **Restart / Play again silently costs a heart.** `resetGame()` calls `startGame()` → `consumeHeart()`. The button gives no sign of this. | `MinesweeperViewModel.kt:699` | Players won't expect it. Add a ♥ cost hint to the button, or confirm when hearts are low. |
| **Navigation state is kept in `remember`, not `rememberSaveable`/Nav.** `var screen by remember { mutableStateOf(Screen.Login) }` | `MainActivity.kt:187` | Rotation, dark-mode switch, or process death sends the user back to Login/Home and drops the game. |

## 2. Gameplay UX

- **The board stays visible while paused.** `PAUSED` shows the full board and only stops the timer, so players can pause, study the board and resume (a timer exploit). Blur or cover the board with a "Paused, tap to resume" overlay.
- **No auto-pause when the app goes to the background.** The timer coroutine keeps running when the app is backgrounded. Hook `Lifecycle.ON_STOP` → `togglePause()`.
- **All difficulties use the same 9×9 grid** (`Difficulty.kt`: 10 / 20 / 30 mines). Hard at 30/81 is mostly guessing rather than logic. Typical progression is 9×9 → 16×16 → 16×30, which also means the board needs **pinch-zoom/pan** (`userScrollEnabled = false` today).
- **Flag mode is not obvious.** The toggle sits between the two counters with no label, and the dig-mode icon is `Construction` instead of a shovel or pickaxe. Add a "DIG / FLAG" text label or a segmented control, and tint the board edge while in flag mode.
- **Long-press flagging gives no haptic feedback in the cell itself.** A haptics setting exists, so check that `toggleFlag` triggers a haptic tick.
- **Cell icons are fixed at 16 dp** and numbers use `bodyMedium`. They don't scale with cell size, so they look tiny on tablets and large boards.
- **The loss banner only says "You hit a mine."** It shows no time, no wrongly placed flags, and no "Back to Home" option. The only action is Play again, which spends another heart.
- **No hint or undo**, even though the economy (coins/diamonds) could support paid consumables such as "reveal a safe cell" or "second chance".

## 3. Home screen

- **Unlabelled icon buttons.** The Tournament and Settings `IconPillButton`s have `contentDescription = null`, so TalkBack users get nothing. The profile avatar is clickable but nothing hints at that.
- **Two trophy icons mean different things.** `EmojiEvents` is used for Tournament (top bar) *and* Achievements (bottom nav).
- **The hero illustration is a fixed 220 dp** next to the title in a `Row`. On narrow phones (≤360 dp wide) it squeezes the "Mines" title column. Use `weight` or `fillMaxWidth(0.45f)` instead.
- **The Start Game button doesn't show its heart cost** and isn't disabled when hearts = 0. Tapping it just opens a dialog.
- **The difficulty cards repeat information** (mine count plus tagline) and don't show grid size or the player's best time for that difficulty, which would add motivation.
- **No "Continue game"**, because quitting always discards the game.

## 4. Placeholder / unfinished features

- **More Games** is just "Coming Soon" yet takes up one of the 4 bottom-nav slots. Hide it or move it into Settings/About until it has content.
- **Tournament** is a full "Coming Soon" screen reached from a prominent home button. "Winter is coming" is also a well-known *Game of Thrones* tagline, so consider original copy.
- **`feature:wallet`** has data only and no UI. Players can't see a transaction or earnings history.
- **No daily reward, streak or onboarding flow.** The economy exists, but nothing brings players back each day. How to Play is only reachable from Settings.

## 5. Visual consistency & theming

- **40 hard-coded `Color(0x…)` values** in feature/app modules (for example `BestTimeGold`, `Color.White` on gold). Some will fail contrast in dark mode or on store themes. Move them into `core:theme` tokens.
- **Mixed button styles.** Some buttons are M3 `Button`s and others are hand-rolled `Box/Row + clickable` (Start Game, Play again, IconPillButton). The hand-rolled ones have no ripple bounds, no disabled state and no `Role.Button` semantics.
- **The Deep Ocean theme only applies to the Game screen.** Other screens use plain `background`, so switching screens feels jarring. The untracked `AmbientOceanBackdrop.kt` suggests this is in progress.
- **Previews only cover the default state.** None exist for dark mode, the Deep Ocean variant, empty states (History "No games played yet"), long usernames, large font scale, or Won/Lost/Paused on the Game screen. Add `@PreviewLightDark`, `@PreviewFontScale` and `@PreviewScreenSizes`. That would have exposed several of the issues above.

## 6. Accessibility

- **36 `contentDescription = null`** across the app. Some are fine (decorative icons), but the interactive ones (Home icon pills, the result banner badge) need labels.
- **Board cells have no semantics.** A screen reader can't announce "Row 3, column 5, hidden / flagged / 2". The game can't be played with TalkBack.
- **Numbers are told apart by colour only** (`CountOne`…`CountEight`). Check the contrast of each against `revealedCellColor` for every store board theme.
- **Small text:** `labelSmall` is used for the level/XP and difficulty taglines. Test at 1.3× font scale, where the XP row in the Home header will likely overflow.

## 7. Localisation

- **`strings.xml` has 4 lines.** Every user-facing string (hundreds) is hard-coded in Kotlin, so translation and RTL support are blocked. Move to `stringResource()` and plurals (for example `"1 heart refilled!"` / `"$missing hearts refilled!"` is hand-rolled pluralisation).
- Times use `"%02d:%02d".format` with no locale. Fine for now, but worth noting.

---

### Suggested priority
1. Wire Game Settings/How-to-Play, fix back on Tournament/Celebration, add a confirmation before leaving a game.
2. Board overlay while paused + auto-pause on background.
3. `rememberSaveable` or Navigation-Compose for screen state.
4. Real difficulty sizes + board zoom.
5. Hide the placeholder tabs, then add a daily reward/streak.
6. Accessibility/strings pass, plus richer previews to catch regressions.
