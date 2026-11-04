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

---
---

# Second audit

How this audit was done: every screen was rendered headlessly at phone size (411×891dp, plus tall renders for scrolling screens) in light mode, with dark-mode renders for Settings and Store, and the suspected bugs were confirmed in code. Dialogs draw in their own window, so the hearts dialog was reviewed from code only. Nothing was tested on a physical device.

## Status of the first audit

| Section | Status |
|---|---|
| 1. Broken interactions | ✅ All fixed: Game Settings / How to Play buttons wired up, back fixed on Tournament and Celebration, confirmation before leaving or restarting, screen state survives rotation. |
| 2. Gameplay UX | ✅ Pause cover, auto-pause, labelled Dig/Flag switch, cell icons that scale, richer loss banner. Flag haptics already worked. Grid sizes kept at 9×9 by decision. ❌ **Hint / undo not built.** |
| 3. Home | ✅ Labels, icons, hero sizing, heart cost, best times, level/XP (it was never wired up), layout and large-font fixes, ambient backgrounds. ❌ **"Continue game" not built**: it needs a decision on the heart-forfeit rule. |
| 4. Placeholder features | ❌ **Not done**: More Games and Tournament are still placeholders, Wallet has no UI, no daily reward or onboarding. |
| 5. Visual consistency | ✅ Colour tokens, one badge-tier palette, proper button semantics, Deep Ocean / Classic ambient scenes across the play flow, multi-previews. |
| 6. Accessibility | ✅ TalkBack board, contrast-checked numbers, "Remove animations" respected, large-font layouts, 48dp touch target. |
| 7. Localisation | ❌ **Not done**: `strings.xml` has 2 entries and only 1 file uses `stringResource()`. |

## New findings

### High: visibly broken or misleading

1. **The win banner covers the board and is badly cramped.** On a new best time, "New best time!" wraps to two lines, the message to four, and the "8s faster than your best" chip to three. The Home icon and "Play again" squeeze the text into about 110dp, and the banner hides the bottom rows of the board. → Put the actions on their own row, shorten the copy, or move the banner below the board. `GameScreen.kt` `ResultBanner`.
2. **Confetti is drawn on top of text.** On the win banner and the Celebration screen, confetti passes over the title, the message and the Continue button, which hurts readability right at the payoff moment. → Draw confetti behind the content, or keep it out of the text area. `ConfettiOverlay` usage in `GameScreen.kt` and `CelebrationScreen.kt`.
3. **Achievement progress bars contradict their labels.** The label shows the absolute count ("5 / 20"), but the bar shows progress within the current tier (5 → 20), so 5/20 displays as an empty bar. The summary also contradicts itself: "5 of 5 unlocked … nice work" counts *tracks*, while the 20% ring counts *tiers*. → Make the label and bar use the same measure, and say what the ring means (e.g. "5 of 25 badges"). `AchievementTrack.progress`, `AchievementScreen.kt:123-160`.
4. **How to Play has no back button.** It takes `onBack` and imports the back-arrow icon but never draws it, so only the system back gesture works. Its title also sits on the top edge with no padding. `HowToPlayScreen.kt`.
5. **How to Play's rules are out of date.** They don't mention the Dig/Flag switch, chording, the heart cost per game, or scoring, and they still use generic Material icons instead of the new flag and mine art.

### Medium: inconsistency and polish

6. **Screen headers are inconsistent.** Settings, History and Feedback use a back arrow plus title, while Store and Achievements have a bare title touching the top edge. Profile's avatar also touches the top. None of these, except Home, have top padding or a camera-cutout inset. → One shared top-bar component.
7. **Profile header is cluttered and ambiguous.**
   - The decorative "1 / 2 / flag" tiles overlap the Settings gear.
   - The five identical orange badge circles next to the name have no labels or explanation.
   - The "Lost" stat uses a red burst icon that reads as a sun.
8. **Profile's signature defaults to fake data.** `ProfileScreen`'s parameters default to demo values (level 8, 320/500 XP, best time "00:42", best times for Easy and Medium). Any caller that forgets a parameter shows invented stats as if they were real. → Neutral defaults such as `null` or empty.
9. **Store purchase UX:**
   - **No previews:** Frost Cells and Robo show generic placeholder icons, so players buy blind.
   - **No affordability state:** a 10-diamond item looks identical whether the player has 3 diamonds or 30. The failure only shows up after tapping.
   - **Price chips don't look tappable,** and nothing says "Buy".
   - **Balances are split:** the header shows only diamonds; coins appear only inside the redeem card.
10. **Settings controls blur together.**
    - A selected theme card and a Sound card that's switched on use the same teal highlight, so on/off settings look like selected radio options.
    - The "selected" marker differs by option: a sync icon on one, a checkmark on another.
    - "App version" is styled as a tappable card but does nothing.
11. **Currency is called two different things.** The hearts dialog says "You have N **gems**", while the rest of the app (Store, Home pills) says **diamonds**. The dialog also offers no route to get more when you can't afford a refill (no link to Store or the coin-to-diamond redeem). `HeartsDialog.kt`.
12. **Login messaging is crowded.**
    - Three competing taglines: "Same logic. New challenges.", "Clear minds / brighter days" and "Think / uncover / win".
    - The logo bomb is different art from the Home hero, and the demo board still uses the stock flag icon.
    - The Google option is a plain icon tile rather than the standard "Sign in with Google" button, which Google's branding guidelines require.
    - Nothing tells guests what they give up (progress isn't saved to an account).
13. **Tournament is a dead end with derivative copy.**
    - "Winter is coming", a well-known *Game of Thrones* tagline, is used twice.
    - There's no action to take, not even "notify me".
    - The header uses a trophy while Home's Tournament button uses a medal.
    - The snowflakes are emoji, which render differently across devices.
14. **History rows are thin.** They show difficulty, result and score, but not the game's duration, which is the stat Minesweeper players care about most. The empty state is plain grey text with no "Play a game" button.
15. **More Games is a blank page.** Just "Coming Soon" text with no header or illustration, and it still takes a bottom-nav slot (carry-over from §4).
16. **The old stock flag icon is still used** in How to Play, Login and Profile's decorative tiles, while the board now uses the new hand-drawn flag.

### Low: housekeeping

17. **Hero image size.** `home_banner.png` and `home_banner_ocean.png` add about 2.2 MB of PNG. → Convert to WebP.
18. **Celebration polish.** Titles like "Gold — Minesweeper Master" are long, and the hero uses a stock trophy icon rather than the actual tier badge.

## Suggested order for the second pass
1. Win banner layout and confetti layering (#1, #2): every win hits these.
2. Achievement progress mismatch (#3) and the How to Play back button and rules (#4, #5).
3. Shared top bar (#6) and Profile cleanup, including the demo defaults (#7, #8).
4. Store affordability and previews, plus "gems" vs "diamonds" (#9, #11).
5. Settings visual separation (#10), Login copy and the Google button (#12), Tournament copy (#13).
6. Carry-overs: localisation, hint/undo, continue game, placeholder tabs, WebP.

## Second audit: status

All 18 findings are fixed. Each one was checked by re-rendering the screen headlessly (light mode, phone size, plus a 360×640 render of the lost game); nothing was tested on a device.

| # | Fix |
|---|---|
| 1 | The win/loss banner sits **below** the board and replaces the Pause/Restart row after the game ends. It's a stacked card: summary on top ("Cleared in 01:12", "Hit a mine at 01:23 · 1 wrong flag"), a short time chip, then full-width **Home** and **Play again −1♥** buttons. The board shrinks to fit the height left, so small phones show everything. |
| 2 | Confetti is drawn **behind** the content on the Game and Celebration screens. Celebration text sits on an opaque card, since transparent text still let confetti show through. |
| 3 | Bar and label use the same measure (`current ÷ next threshold`, so 5/20 shows 25%), fixed in `AchievementTrack.progress` itself. The summary reads "N of M **badges** earned" to match the ring, and the stray end-dot on the bars is gone. |
| 4 | How to Play has a back button, via the new shared top bar. |
| 5 | How to Play is now **Rules / Controls / Hearts & scoring**: the Dig/Flag switch, long-press flagging, chording, the heart cost and refund, regeneration, and scoring, all checked against the code. It uses the board's own flag and mine art. |
| 6 | New `ScreenTopBar` in `core:ui` (back arrow, title, actions, top spacing, cutout inset), used by Store, Achievements, Settings, History, Feedback, How to Play, Tournament and More Games. Profile got the same top spacing. |
| 7 | Profile: the decoration over the Settings gear is removed, "Lost" uses a broken heart instead of a sun, and the badge row has bigger icons, an "N badges ›" label, and opens Achievements. |
| 8 | `ProfileScreen` defaults are neutral (level 1, 0 XP, no best times). Real data was already passed in, so behaviour is unchanged; it just can't show invented stats anymore. |
| 9 | Store items have drawn previews (frosted cells, a Robo face). Affordable items show a filled **Buy 💎N** button; unaffordable ones show a muted price and "Need N more"; owned themes say **Apply**. The header shows both coin and diamond balances. |
| 10 | Settings is split into **Theme** and **Sound & haptics**. Toggle cards stay neutral when on (the switch shows the state), the whole row toggles and is announced as a switch, every theme option uses a checkmark, and App version is a plain row. |
| 11 | "Gems" is now "diamonds" everywhere, including three Store and refill messages. When you can't afford a refill, the hearts dialog offers **Get diamonds**, which opens the Store. |
| 12 | Login: one message (the Home hero art plus "Mines / Think. Uncover. Win."), a standard **Sign in with Google** button in Google's light/dark styles, and "Continue as guest" with "Guest progress is saved on this device only". The extra taglines and the duplicate demo board are gone. |
| 13 | Tournament: "The Winter Cup" replaces "Winter is coming", there's a **Play a practice game** button (starts a game at the selected difficulty), the shared top bar, and real snowflake icons instead of emoji. |
| 14 | History rows show **duration · time of day**, and Profile's preview rows show duration. The empty state has a **Play a game** button. |
| 15 | More Games has the shared header, an illustration, clearer copy and a **Play Mines** button. |
| 16 | `BoardArt` moved to `core:ui`, so Login, How to Play and Profile use the board flag. The only stock flag left is the Dig/Flag switch's control icon, which is intended. |
| 17 | The hero images moved to `core:ui` as WebP: **2.2 MB → 263 KB**, with no visible difference. |
| 18 | Celebration shows the actual badge (track icon in tier colour), with the tier in the small heading ("GOLD BADGE UNLOCKED") and just the track name as the title. Gold text is darkened on white cards so it's readable. The track-to-icon mapping is now one shared function (`achievementTrackIcon`). |

**Still open (carry-overs from the first audit):** localisation, hint/undo, "Continue game", and Wallet UI / daily reward / onboarding.
