# Mines codebase engineering audit (feature completeness, correctness, release blockers)

Method: read-only review of the repo at commit 13c9803 (master, clean, 105 commits). Every claim cites `path:line` relative to the repo root `C:\Users\hp\AndroidStudioProjects\Mines`. No Gradle build was run, so "compiles" and "runs" were not checked, and nothing was tested on a device. About 15.7k lines of Kotlin in total; the largest files are GameScreen.kt (1274), MainActivity.kt (1133), ProfileScreen.kt (1041), HomeScreen.kt (987) and MinesweeperViewModel.kt (830).

## 1. Feature inventory and completeness

### Takeaway
The core loop works end to end: Google or guest login, Home, Game, result, Celebration, History/Profile/Achievements, Store, Hearts and Settings. Several things are only partly there or are placeholders: Tournament, More Games, wallet UI, the Robo avatar and Frost skin purchases, feedback screenshots, daily reward/onboarding, localisation, and analytics events.

### Cited Findings
| Feature / screen | Rating | Evidence |
|---|---|---|
| Login (Google via Credential Manager + "Continue as guest") | Complete | `feature/auth/.../GoogleAuthManager.kt:147-226`; wiring `app/.../MainActivity.kt:556-672`; doc.md:272 (standard Sign in with Google button) |
| Home (difficulty pick, hearts, coins/diamonds, level/XP, best times) | Complete | `MainActivity.kt:676-718`; HomeScreen.kt (987 lines) |
| Game board (dig/flag switch, long-press flag, chording, pause cover, auto-pause on background, leave/restart confirmation, TalkBack cell semantics) | Complete (core) | `feature/game/.../ui/GameScreen.kt:150-180` (confirm dialogs), `:971,1001` (pause cover clears semantics), `:1121-1124` (per-cell contentDescription); `MainActivity.kt:184-187` (onStop auto-pause) |
| Hint / undo / "Continue game" / custom sizes | Absent | doc.md:193-194, 280 |
| How to Play | Complete | `MainActivity.kt:904-910`; doc.md:265 |
| History (filter by difficulty) | Complete, but capped at 100 games for signed-in users | `MainActivity.kt:1092-1108`; `FirestoreGameRepository.kt:130-139` (limit 100) |
| Profile (stats, avatars, best times) | Complete | `MainActivity.kt:1017-1047`; 6 local avatars in `feature/profile/src/main/res/drawable/avatar_*.xml` |
| Achievements (5 tracks × 5 tiers) | Complete, but some tiers can't be reached when signed in (see §5) | `feature/achievements/.../AchievementCalculator.kt:56-122` |
| Celebration (level-up / tier unlocked queue) | Complete | `MinesweeperViewModel.kt:644-679`; `MainActivity.kt:316-324, 1004-1012` |
| Hearts (indicator, dialog, regen, diamond refill, "Get diamonds" link to Store) | Complete | `feature/life/...`; `MainActivity.kt:1114-1131`; 14 unit tests `feature/life/src/test/.../LifeCalculatorTest.kt` |
| Store (4 catalog items, buy/apply, coin→diamond redeem) | Partial | `StoreCatalog.kt:16-21`; Robo avatar does nothing once bought (`MinesweeperViewModel.kt:326-329`); Frost Cells has no art (`catalog/FrostCellSkin.kt` comment) and in the game it only thickens/whitens the cell border (`GameScreen.kt:1107-1108`) |
| Wallet | Partial (data only, no transaction/earnings UI) | `feature/wallet` contains only `data/` packages; doc.md:149, 195 |
| Settings (theme light/dark/system, sound, haptics, sign out, delete account, app version) | Complete; no privacy policy or terms link | `SettingsScreen.kt:93, 318-322, 347-390`; a grep for "privacy\|policy\|terms" over all *.kt files returns nothing |
| Feedback form | Partial: screenshots can be picked but are never uploaded, only counted | `FeedbackScreen.kt:95-104, 238-275`; `MinesweeperViewModel.kt:726-733` (`screenshotCount = data.screenshots.size`); `FirestoreFeedbackRepository.kt:13-29` |
| Tournament | Placeholder ("COMING SOON" + a practice-game button) | `TournamentScreen.kt:141-230`; `MainActivity.kt:1077-1087` |
| More Games | Placeholder (takes a bottom-nav slot) | `MoreGamesScreen.kt:73-100`; `MainActivity.kt:540-543` |
| Daily reward / streak / onboarding | Absent | doc.md:150, 195, 280 |
| Leaderboards / social | Absent | no code; Tournament is a placeholder |
| Localisation | Absent | `app/src/main/res/values/strings.xml` has 2 strings (app_name, google_web_client_id); only `MainActivity.kt:240` uses `stringResource` (grep) |
| Analytics | Library only, no events | `app/build.gradle.kts:35` includes firebase-analytics; grep for `logEvent`/`FirebaseAnalytics` finds no usage |
| Ads / billing / IAP | Absent (by design) | no dependency in `gradle/libs.versions.toml` |

### Inferences
- The visible UI is polished: doc.md records two audits, and every item from the second one is marked fixed (doc.md:255-278). What's missing is behind the UI: retention features, social/competition, and server-side integrity.
- There are two purchases that look broken to players: Robo, which delivers nothing, and Frost, which is barely visible. That is a refund and review risk if diamonds ever become purchasable.

### Gaps
- Rendering and on-device behaviour weren't verified; doc.md also states that nothing was tested on a device (doc.md:186, 257).

## 2. Engine correctness (feature/game/domain)

### Takeaway
The engine is small and mostly correct: first click is safe and opens an area, BFS flood fill, standard chording, and win when every safe cell is revealed. It has no seeding, no replay and no no-guess generation. All three difficulties use a 9×9 grid, so Hard is mostly guessing. There's one first-click-safety edge case, and the ViewModel timer can be frozen with pause. The engine has no tests.

### Cited Findings
- **First-click safety.** Mines are placed lazily on the first `reveal`, excluding the tapped cell and its 8 neighbours (`MinesweeperGame.kt:21, 37-60, 125-127`). If there isn't room, it falls back to excluding only the tapped cell (`:45-50`).
- **Edge bug.** `reveal()` places mines before checking `isFlagged` (`MinesweeperGame.kt:125-133`). If the player flags a cell first and then taps that flagged cell (a no-op), the mines get placed around the flag, and the player's real first tap is no longer guaranteed safe. The UI allows flagging before the first reveal (`toggleFlag` `:246-263` has no `minesPlaced` guard).
- **Randomness.** `candidates.shuffled(Random)` uses the default unseeded Kotlin Random (`MinesweeperGame.kt:52`). There's no seed parameter, so there's no determinism, daily seed, replay or reproducible bug reports. There's no solver or no-guess generation anywhere in the repo.
- **Flood fill.** BFS with a visited set that stops at mines/flags and expands only from zero cells (`MinesweeperGame.kt:149-198`). Correct.
- **Chording.** Requires flagged-neighbour count == cell number, reveals unflagged hidden neighbours and returns the first detonation (`MinesweeperGame.kt:212-244`). The ViewModel calls chord when a revealed cell is tapped (`MinesweeperViewModel.kt:458-462`).
- **Flags.** No limit on flag count (`MinesweeperGame.kt:246-263`). Flags aren't required to win.
- **Win / loss.** `isWon()` = no unrevealed safe cells (`MinesweeperGame.kt:276-281`). On loss, `revealAllMines()` (`:265-274`) runs and status becomes LOST with `detonatedCellIndex` (`MinesweeperViewModel.kt:464-485`). Wrong flags are crossed out in the UI (`GameScreen.kt` comment near :1131).
- **Difficulty.** EASY 9×9/10, MEDIUM 9×9/20, HARD 9×9/30 (`Difficulty.kt:5-7`). Mine densities are 12% / 25% / 37%. doc.md:193 says grid sizes were kept at 9×9 on purpose.
- **Timer.**
  - It's a coroutine that loops on `delay(1.seconds)` and increments a counter (`MinesweeperViewModel.kt:420-445`), not a wall-clock delta.
  - It starts at `beginGame`, before the first click (`:364-384`).
  - Pause cancels the job, and resume starts a fresh 1 s delay (`:393-418`). Partial seconds are thrown away, so pausing and resuming faster than once a second stops the clock. That's a best-time exploit, even though the board is hidden while paused.
  - At 30 minutes the game auto-quits to Home without recording a result (`:62, 435-437`), and the heart is lost.
- **State.** `GameState` holds READY/PLAYING/PAUSED/WON/LOST (`GameState.kt`). READY is never used, because `beginGame` sets PLAYING (`MinesweeperViewModel.kt:380`).
- **Score** (`ScoreCalculator.kt:4-66`).
  - Wins use a time step function: Easy 100/70/40 at ≤60 s / ≤120 s / slower; Medium 200/150/90 at 90/180 s; Hard 350/250/150 at 120/240 s.
  - Losses score +20/+40/+70 if ≥50% of safe cells were revealed, otherwise −30/−50/−80.
  - Totals are floored at 0 over time (`FirestoreGameRepository.kt:90-96`; `GameRepositoryImpl.kt:298-312`).
- **XP / level** (`LevelSystem.kt:2-5, 19-27`). XP = wins×100 + losses×10, the same on every difficulty. XP needed for level L = round(100·L^1.5, nearest 10). Level is recomputed from the history list every time (`MinesweeperViewModel.kt:684-691`; `MainActivity.kt:398-404`).
- **Tests.** The only unit tests are `LifeCalculatorTest.kt` plus the template `app/src/test/.../ExampleUnitTest.kt` and `androidTest/ExampleInstrumentedTest.kt` (git ls-files). `MinesweeperGame`, `ScoreCalculator`, `LevelCalculator` and `AchievementCalculator` have no tests.

### Inferences
- Losing still earns XP (10 per loss), and Easy wins give the same XP as Hard wins. The fastest way to level up is spamming Easy, which works against the per-difficulty coin scaling.
- Because the timer counts ticks rather than measuring time, the stored `durationSeconds` can't be trusted for best times or any future leaderboard or tournament.
- Without seeds, neither tournaments nor daily challenges can be fair: players can't be given the same board.

### Gaps
- Didn't measure how often 9×9/30 boards force guesses. There's no solver in the repo to measure it with.

## 3. Economy: wallet, hearts, store, scoring (balance and cheat resistance)

### Takeaway
The economy is entirely client-authoritative. Balances change through client writes to `users/{uid}`, and time-based rules (heart regen, the 2-per-day redeem cap) use the device clock even for signed-in users. The repo contains no Firestore security rules. The balance is very tight: the only diamond source is 2 redemptions a day, and one refilled heart costs 2.5 days of redemptions.

### Cited Findings
- **Coins.** Earned only on a win: 50/100/200 for Easy/Medium/Hard (`MinesweeperViewModel.kt:68-72, 550-553`). Losses earn 0 coins. The `wallet.addCoins` launch has no try/catch (`:551-553`).
- **Diamonds.** The only source is redeeming 500 coins for 1 diamond, at most 2 per rolling 24 h (`CoinWalletDataStore.kt:38-40, 84-118`; Firestore twin `FirestoreWalletRepository.kt:375-408`). There are no IAP, ads or daily rewards.
- **Hearts.** Max 5, regenerate 1 per hour, refill costs 5 diamonds per missing heart (`LifeRules.kt:5-19`). A heart is spent when a game starts and refunded on a win (`MinesweeperViewModel.kt:337-362, 555-562`), so only losses and quits cost hearts.
- **Store prices** (diamonds): Deep Ocean 10, Frost Cells 20, Robo 15 (`feature/store/.../catalog/*.kt`). The `StoreItem.price` KDoc wrongly says "coins" (`StoreItem.kt:13`).
- **Clock — hearts.** Both stores use `System.currentTimeMillis`: `LifeDataStore.kt:149`, `FirestoreLifeRepository.kt:287, 318, 334, 342`. The calculator already clamps a clock set backwards (`LifeCalculator.kt:18-20`, covered by a test), but setting the clock forward grants hearts instantly, even for signed-in users.
- **Clock — redeem cap.** Timestamps come from the device clock (`CoinWalletDataStore.kt:110, 166`; `FirestoreWalletRepository.kt:399, 445`), so changing the clock resets the 2-per-day cap.
- **Client writes.** `FieldValue.increment` on coins/diamonds (`FirestoreWalletRepository.kt:337-353`), `arrayUnion` on ownedStoreItemIds (`FirestoreStoreRepository.kt:310-315`), and direct writes of `hearts`/`heartsRegenAnchor` (`FirestoreLifeRepository.kt:340-345`). There are no Cloud Functions, and there is no `firestore.rules` or `firebase.json` in the repo (find over the tree returned nothing).
- **Guest storage.** Guest wallet, hearts and inventory live in plain Preferences DataStore files (`CoinWalletDataStore.kt:11`, `LifeDataStore.kt:144`, `StoreDataStore.kt` "store_inventory"). `android:allowBackup="true"`, and the backup rules are untouched templates (`app/src/main/AndroidManifest.xml`; `res/xml/backup_rules.xml`, `data_extraction_rules.xml`).
- **Purchase isn't atomic.** It spends diamonds in one transaction, writes ownership separately, and refunds in a catch block (`MinesweeperViewModel.kt:295-317`). If the process dies between those steps, the diamonds are lost without the item. `refillHearts` follows the same pattern (`:795-809`).
- **Balance check.** Easy needs 10 wins per diamond and Hard needs 2.5 wins. The 2-per-day cap means at most 2 diamonds a day. One refilled heart costs 5 diamonds, which is 2.5 days of maximum redemptions, while a free heart regenerates every hour. The full catalog costs 45 diamonds: at least 23 days and 22,500 coins (450 Easy wins or 113 Hard wins).

### Inferences
- Refilling with diamonds is almost never worth it next to 1-per-hour regen, so diamonds will mostly go to the 3 cosmetics. After those, the economy has nothing left to spend on.
- Any signed-in user with the Firebase JS SDK and the app's committed config (`app/google-services.json`) could set arbitrary balances unless the rules configured in the console restrict field changes. Rules alone can't validate "earned coins legitimately"; that needs server-side game validation or Cloud Functions.

### Gaps
- The live Firestore rules in the Firebase console couldn't be inspected because they aren't in the repo. Whether they're locked down or wide open is unknown.

## 4. Guest (Room) vs signed-in (Firestore): sync, migration, conflicts, offline

### Takeaway
Each repository picks Room/DataStore or Firestore per call, based on whether a Firebase user exists. Guest data moves to the account only once, on the account's very first sign-in. The code comments promise a retry that can never happen. There's no merge for existing accounts and no outbox. Signed-in users effectively can't play offline, and Firestore errors aren't caught, so they crash the app.

### Cited Findings
- **Session.** The session is derived from `FirebaseAuth.currentUser` (`core/session/.../SessionManager.kt:11-14, 16-30`). Guest means no Firebase user; there is no anonymous auth. The UI profile is a separate DataStore cache (`UserSessionStore.kt:13-41`) that decides whether to skip Login (`MainActivity.kt:336-346`), so the UI and the data layer can disagree about who is signed in.
- **Per-call routing.** `GameRepositoryImpl.kt:220-259`, `WalletRepositoryImpl.kt:225-294`, `LifeRepositoryImpl.kt:240-266`, `StoreRepositoryImpl.kt` (isOwned/addOwnedItem/equip). Flows switch with `flatMapLatest` on the auth listener (`WalletRepositoryImpl.kt:200-217`).
- **Room.** `version = 1`, `exportSchema = false`, no migrations and no fallback (`GuestGameDatabase.kt:6-30`). One table, `guest_games`, with no row limit (`GuestGameDao.kt`).
- **Firestore layout.**
  - `users/{uid}` holds profile, aggregate stats, coins/diamonds/redeemTimestamps (`FirestoreGameRepository.kt:38-58`), hearts (`FirestoreLifeRepository.kt:290-293`) and the store inventory.
  - Game history is in `users/{uid}/games`.
  - Feedback is in a top-level `feedback` collection (`FirestoreFeedbackRepository.kt:24`).
- **Migration** (`MainActivity.kt:570-648`).
  - It runs only when `ensureUserDocument` returns true, meaning the user document didn't exist (`FirestoreGameRepository.kt:26-63`).
  - Games are replayed one transaction each (`:182-196`), the wallet is incremented, store items are unioned, and then local data is cleared.
  - Hearts are not migrated.
- **Retry promise that can't be kept.** On failure, the comments say "it'll be retried on the next sign-in attempt" (`MainActivity.kt:595-598, 622, 645`). But the user document is created before migration (`FirestoreGameRepository.kt:61`), so on the next sign-in `isFirstTimeAccount` is false and the migration never runs again. A partial game upload also leaves every local game in place, so games end up duplicated or orphaned.
- **Existing accounts.** If a guest signs in to an account that already exists, nothing is merged or cleared. The guest data is still there, and comes back, after sign-out.
- **No error handling around sign-in follow-ups.** `ensureUserDocument` and the migration run in `scope.launch` without try/catch (`MainActivity.kt:561-581`). The profile is already saved (`:572`), so a network error at that point crashes the app.
- **Uncaught exceptions → crashes.**
  - `completeLevel` runs `getGameHistory` and the `saveGameResult` transaction without try/catch (`MinesweeperViewModel.kt:586-626`).
  - `redeemDiamond` and `refreshRedeemStatus` have no try/catch (`:748-760`).
  - The history/stat loads in `LaunchedEffect(screen)` have no try/catch (`MainActivity.kt:348-396`).
  - Snapshot listeners call `close(error)` (`FirestoreWalletRepository.kt:321-323`; `FirestoreLifeRepository.kt:299-301`; `FirestoreStoreRepository.kt` observeStore), and the ViewModel collectors don't catch (`MinesweeperViewModel.kt:224-246`).
- **Offline for signed-in users.**
  - `consumeHeart` is a transaction (`FirestoreLifeRepository.kt:315-327`). Firestore transactions need the server, so starting a game offline fails with "Couldn't start the game" (`MinesweeperViewModel.kt:349-354`).
  - `getStatus`/`getRedeemStatus`/`getStatistics` use `Source.SERVER` (`FirestoreLifeRepository.kt:311`; `FirestoreWalletRepository.kt:356`; `FirestoreGameRepository.kt:126`).
  - Guests work fully offline.
- **100-game cap.** Signed-in history is `limit(100)` (`FirestoreGameRepository.kt:130-139`). Level/XP (`MainActivity.kt:398-404`; `MinesweeperViewModel.kt:684-691`) and achievements (`MainActivity.kt:440-442`) are computed from that list rather than from the aggregate counters in `users/{uid}`.
- **Feedback for guests.** Guests write feedback with no auth (`FirestoreFeedbackRepository.kt:13-29`; `userId` is null for guests, `MainActivity.kt:889-892`).

### Inferences
- Once a signed-in user passes 100 games, their level freezes or even drops as old wins fall out of the window. "Games Played" DIAMOND (200) and "Games Won" DIAMOND (150) can never be reached (`AchievementCalculator.kt:62-81`), and Celebration diffs are computed on that truncated list too.
- The Firestore rules have to allow unauthenticated `create` on `feedback` for guest feedback to work. That's a spam/abuse vector, and the rules aren't in the repo, so this can't be checked.
- There's no conflict model. With two devices on one account, each reads and writes Firestore directly; transactions keep counters consistent, but nothing reconciles anything else.

### Gaps
- Whether Firestore offline persistence is customised: no `FirebaseFirestoreSettings` was found, so the SDK defaults apply.

## 5. Architecture: DI, ViewModel/state, process death, module boundaries, tests

### Takeaway
It's a multi-module layout with no DI framework. One 830-line AndroidViewModel builds every repository, and MainActivity builds a second set of repositories for sign-in and deletion. Navigation is a hand-rolled `when` over a saveable `Screen`. Game state doesn't survive process death. Module isolation is weakened by `api` leaks. Test coverage is close to zero.

### Cited Findings
- **Manual construction ("DI").**
  - `MinesweeperViewModel` directly creates SessionManager, SettingsDataStore, GameFeedback and all the repositories, including `FirebaseFirestore.getInstance()` defaults (`MinesweeperViewModel.kt:78-112`).
  - MainActivity also creates `GoogleAuthManager`, `UserSessionStore`, `FirestoreGameRepository`, `FirestoreWalletRepository`, `CoinWalletDataStore`, `FirestoreStoreRepository`, `StoreDataStore` and `GuestGameRepository` in `remember` (`MainActivity.kt:198-230`).
  - Business logic (migration, account deletion) sits in composable lambdas (`MainActivity.kt:561-835`).
- **README is out of date.** It describes "one 650-line class" (README.md:95-101); the file is now 830 lines (wc -l). The suggested per-feature ViewModel split hasn't been started.
- **Navigation.** `rememberSaveable` with a custom `ScreenSaver` (`MainActivity.kt:104-144, 244-246`). This fixes rotation (doc.md:192). History, statistics and loading flags use plain `remember` (`:262-280`), and so does the flag-mode toggle (`GameScreen.kt:153`).
- **Process death.**
  - `Screen.Game` is restored, but `gameState` lives only in the ViewModel (`MinesweeperViewModel.kt:75, 137`), with no SavedStateHandle or persistence.
  - The Game branch then sets `screen = Screen.Home` during composition (`MainActivity.kt:995-998`), which is a side effect inside composition.
  - The heart spent on that game is lost (that behaviour is intended, `MinesweeperViewModel.kt:337-344`).
- **State plumbing.** About 25 separate `MutableStateFlow`s rather than a UI-state object (`MinesweeperViewModel.kt:116-191`). User messages go through Toasts.
- **Module graph** (`settings.gradle.kts:27-47`). There are feature→feature edges: `:feature:game → :feature:store` (`feature/game/build.gradle.kts:22`), `achievements → api(game)`, `celebration → api(achievements)`, `profile → api(achievements, game)`, `home → api(profile, game, life)`. Third-party libraries are exposed via `api`: Firebase, Room, DataStore, Credentials, icons (`core/theme/build.gradle.kts:24-28`; `feature/auth/build.gradle.kts:27-33`; `feature/game/build.gradle.kts:31-35`). That matches the README's open "Known simplification" (README.md:81-91), which is still unresolved.
- **Tests.** One real test class (`feature/life/src/test/.../LifeCalculatorTest.kt`, 14 tests). Only `feature/life` declares `testImplementation` (`feature/life/build.gradle.kts:33`). The `app` test and androidTest files are templates.
- **Build workaround.** `android.disallowKotlinSourceSets=false` with suppressed warnings, for KSP/Room on AGP 9 built-in Kotlin (`gradle.properties:20-25`).

### Inferences
- Moving to Hilt/Koin or simple constructor injection would make the repositories testable. Today the Firestore-touching classes can't be unit-tested without Robolectric or the emulator.
- `feature:game` depending on `feature:store` (for board theme style) breaks the README rule of "core never depends on feature; features depend only where there's a product relationship" in spirit, and makes store changes recompile game.

### Gaps
- No Gradle `buildHealth`/dependency-analysis was run, so unused `api` declarations weren't enumerated.

## 6. Release readiness

### Takeaway
It isn't ready for release. R8 is off, there's no signing config, versionCode is 1, there's no crash reporting, there are no privacy policy or consent hooks, and backups are unrestricted. Firestore rules aren't version-controlled and every string is hardcoded. Account deletion does exist and is mostly correct.

### Cited Findings
- **SDK levels.** `compileSdk = release(37)`, `minSdk = 24`, `targetSdk = 37` (`app/build.gradle.kts:7-16`). All library modules use the same compileSdk/minSdk (module build files, line 7-9).
- **Toolchain.** AGP 9.4.0 (`gradle/libs.versions.toml:2`), downgraded from 9.5.0 in commit 5915346 on 5 Nov 2026 (`git show 5915346`; the commit message gives no reason). Gradle 9.6.0 wrapper (`gradle/wrapper/gradle-wrapper.properties`), Kotlin 2.4.21, KSP 2.3.13, Compose BOM 2026.09.00, Firebase BOM 35.1.0, Room 2.8.5. JVM target 11 (`app/build.gradle.kts:28-31`).
- **R8 / minify.** `release { optimization { enable = false } }` (`app/build.gradle.kts:21-26`). `app/src/main/keepRules/rules.keep` is the untouched template. So there's no shrinking or obfuscation, and the APK will be large with the extended icons library.
- **Signing / versioning.** No `signingConfigs` block. Keystores are git-ignored (`.gitignore` "*.jks/*.keystore"). `versionCode = 1`, `versionName = "1.0"`.
- **Crash reporting.** No Crashlytics or any other reporter (`gradle/libs.versions.toml:31-42`). The app has no logging at all (grep for `Log.` returns nothing).
- **Privacy.** No privacy policy link or text anywhere (grep). firebase-analytics is included and collects automatically, with no consent UI or opt-out found. Feedback collects name and email into Firestore (`FirestoreFeedbackRepository.kt:13-23`).
- **Account deletion.**
  - Present, with a confirmation dialog (`SettingsScreen.kt:355-390`). It deletes the games subcollection in 400-document batches and the user document (`FirestoreGameRepository.kt:151-164`), then the Auth user, re-authenticating if needed (`GoogleAuthManager.kt:256-314`).
  - Feedback documents aren't deleted.
  - Data is deleted before the Auth user, so a cancelled re-auth leaves an account with its data already gone (`MainActivity.kt:783-823`).
  - The guest wallet/life/store/settings DataStores aren't cleared.
- **Backup.** `allowBackup="true"` with template `backup_rules.xml` / `data_extraction_rules.xml`. Everything is backed up, including the guest economy and the cached session profile.
- **Committed config.** `app/google-services.json` is in the repo, and the web client ID is hardcoded in `strings.xml`. That's normal for Firebase, but it makes server-side rules mandatory.
- **Hardcoded strings.** Effectively 100% (§1). doc.md:168, 198 lists localisation as not done. 49 occurrences of `contentDescription = null` remain (grep), most of them decorative according to doc.md:197.
- **Insets.** Status and navigation bars are hidden globally (immersive) (`MainActivity.kt:151-163`), and cutouts are handled per screen (commit 13c9803).

### Inferences
- Minimum release work: turn on R8 with Firebase/Room keep rules verified, add a signing config (or Play App Signing), add Crashlytics, add a privacy policy URL in Settings and on the store listing, write and deploy Firestore rules, and restrict backup rules.

### Gaps
- Play policy specifics (Data safety form, deletion URL requirement, target API deadlines) are left to the separate policy track.

## 7. Status of README.md / doc.md open items

### Takeaway
Both README "next steps" are still open. Every doc.md audit item is fixed except the carry-overs it lists itself: localisation, hint/undo, Continue game, and wallet UI/daily reward/onboarding.

### Cited Findings
- README "Known simplification" (api→implementation) is **open**: `api(...)` is still in 12 module build files (grep output, e.g. `feature/game/build.gradle.kts:31-35`).
- README "Suggested next step" (split MinesweeperViewModel per feature) is **open**: there's still one `app/.../MinesweeperViewModel.kt` (830 lines), and no feature module has its own ViewModel (git ls-files shows no `*ViewModel.kt` outside app).
- README's module map leaves out `:feature:life` (README.md:22-36 vs `settings.gradle.kts:39`). The README is stale.
- doc.md first audit: sections 1, 2, 3, 5 and 6 are fixed. Still open: hint/undo, Continue game, placeholder features, localisation (doc.md:190-198, 280).
- doc.md second audit: all 18 marked fixed (doc.md:255-278). The code spot-checks agree: Tournament now says "THE WINTER CUP" (`TournamentScreen.kt:141-149`), More Games has a "Play Mines" button (`MoreGamesScreen.kt:100`), and confirmation dialogs exist (`GameScreen.kt:157-180, 365`).
- doc.md 2.x item "the board stays visible while paused" is fixed: the board is covered and its semantics cleared (`GameScreen.kt:971, 1001`). The pause/resume tick-drop exploit (§2) isn't mentioned in doc.md and is still open.

### Inferences
- doc.md only covers UI/UX. None of the backend, economy-integrity, crash or release items in this audit appear in either document.

### Gaps
- None.

## 8. Top 15 risks, ranked (effort: S ≤1 day, M = days, L = 1+ week)

### Takeaway
The biggest blockers are integrity (no rules in the repo, client-authoritative economy, device-clock timers), stability (uncaught Firestore exceptions with no crash reporting), and release hygiene (R8 off, no signing, no privacy policy). Next come data-correctness bugs: the 100-game cap and a migration retry that never happens.

### Cited Findings
1. **No Firestore security rules in the repo, and a fully client-authoritative economy and stats.** No `firestore.rules`; client writes coins/diamonds/hearts/inventory/stats (`FirestoreWalletRepository.kt:337-353`, `FirestoreLifeRepository.kt:340-345`, `FirestoreGameRepository.kt:82-122`). Effort: **M** to write and deploy field-validated rules; **L** to move awards to Cloud Functions.
2. **Uncaught Firestore/network exceptions crash the app.** Locations: `MinesweeperViewModel.kt:551-553, 586-626, 748-760, 224-246`; `MainActivity.kt:348-396, 561-581`; listener `close(error)`. Effort: **S–M** (add a CoroutineExceptionHandler, try/catch, and `.catch {}` on flows).
3. **No crash reporting or logging.** No Crashlytics (`libs.versions.toml`), no `Log` calls. Effort: **S**.
4. **Release build hygiene.** R8 disabled (`app/build.gradle.kts:21-26`), template keep rules, no signingConfig, versionCode 1. Effort: **S–M** (enabling R8 needs a smoke test).
5. **No privacy policy or analytics consent; account deletion leaves feedback PII and deletes data before the Auth user** (`MainActivity.kt:783-823`; `FirestoreFeedbackRepository.kt`). Effort: **S–M**.
6. **The 100-game history cap breaks level, achievements and celebrations for signed-in users.** Some tiers become impossible, and level can go down (`FirestoreGameRepository.kt:130-139`; `AchievementCalculator.kt:62-81`). Effort: **M** (store XP and achievement counters in the user document, or page through the full history).
7. **Device-clock trust in heart regen and the redeem cap, even signed-in** (`FirestoreLifeRepository.kt:287`; `FirestoreWalletRepository.kt:399, 445`). Effort: **M** (use server timestamps via `FieldValue.serverTimestamp` plus rules, or Functions).
8. **Signed-in users can't start a game offline** (transaction in `consumeHeart`, `FirestoreLifeRepository.kt:315-327`), and results are lost if the connection drops mid-game (no outbox). Effort: **M**.
9. **Guest→account migration.** The retry the comments promise is impossible, partial uploads duplicate or orphan games, there's no merge for existing accounts, and hearts aren't migrated (`MainActivity.kt:574-648`; `FirestoreGameRepository.kt:61`). Effort: **M** (store a migration flag/state on the user document and make it idempotent with per-game IDs).
10. **Game state is lost on process death and at the 30-minute auto-quit** (heart lost, no result saved), and the screen is mutated during composition (`MinesweeperViewModel.kt:62, 435-437`; `MainActivity.kt:995-998`). Effort: **M** (serialise board and seed to SavedStateHandle/DataStore, which also enables "Continue game").
11. **Timer integrity.** Rapid pause/resume freezes the tick counter, and the clock starts before the first click (`MinesweeperViewModel.kt:393-445`). Best times and any future tournament can be gamed. Effort: **S** (use an elapsed-realtime delta).
12. **Unrestricted backup plus plain-DataStore guest economy** allows restore- or edit-based tampering and restores a stale session cache (`AndroidManifest.xml` allowBackup; template xml rules). Effort: **S**.
13. **Economy balance and dead purchases.** Diamonds capped at 2 a day, refill at 5 per heart, Robo delivers nothing, Frost is barely visible (`LifeRules.kt:8`; `CoinWalletDataStore.kt:38-39`; `MinesweeperViewModel.kt:326-329`; `GameScreen.kt:1107-1108`). Effort: **S** to rebalance or hide; **M** to ship the art.
14. **Engine gaps.** No seed/determinism or no-guess mode, all difficulties 9×9 (Hard at 37% density), and the first-click-on-flag safety bug (`MinesweeperGame.kt:52, 125-133`; `Difficulty.kt:5-7`). Effort: **S** for the bug and the seed parameter; **L** for no-guess generation with a solver.
15. **Maintainability and test debt.** 830-line god ViewModel, manual DI, business logic in composables, `api` leaks, a single test class, and Room `exportSchema=false` with no migration strategy (`MinesweeperViewModel.kt`; `MainActivity.kt:561-835`; `GuestGameDatabase.kt:6-30`). Effort: **L**; **M** for engine and calculator unit tests alone.

Lower-priority notes: all strings hardcoded (L); two placeholder tabs, one occupying a bottom-nav slot (S to hide); feedback screenshots silently dropped (S to remove or M to upload to Storage); guest feedback needs unauthenticated Firestore writes (S, via anonymous auth or a rules review); the AGP 9 `disallowKotlinSourceSets=false` workaround (S, track KSP).

### Inferences
- Items 1, 2, 3, 4 and 5 block launch. Items 6–11 hurt data correctness and fairness and should be fixed before tournaments or leaderboards (the Tournament placeholder) are built, since those need seeds, trustworthy timers and server-validated results.

### Gaps
- Effort estimates are judgement calls based on code size and structure, not measured.
- Behaviour against the live Firebase project (rules, indexes, quotas) is unknown.
