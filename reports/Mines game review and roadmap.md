# Mines needs fairness before it chases fame

Mines is a well-polished classic Minesweeper that is not ready to launch and does not yet stand out. Its Compose UI, ocean art direction, haptics and TalkBack cell labels are better than most clones. But the game is narrow (**every difficulty is the same 9×9 board, so Hard is 37% mines and mostly guessing**), novelty is low (**about 3/10**), and the backend is fragile: **the economy is fully client-authoritative, there are no Firestore rules in the repo, uncaught network exceptions crash the app, R8 is off, and there's no crash reporting, privacy policy or analytics events**. The market is a crowded long tail. The biggest confirmed Minesweeper listing on Google Play has about 5M installs, most clones sit at 10K–500K after years, and the base-case 12-month outlook without paid acquisition is about **20K installs, a few hundred DAU and $3–5K gross**. Three product decisions hold the game back more than craft does: a heart "entry fee" in a genre where every rival is free and unlimited, Login on every cold start for guests, and signed-in players being unable to start a game offline. The path forward has six milestones over about 12 months. Fix stability, integrity and compliance first. Then make every loss fair (seeded no-guess boards, bigger boards, hearts out of classic play). Then build the habit loop (a daily seeded "Tide" puzzle with streaks), and only after that add monetization, tournaments and a signature ocean-themed mechanic. Each milestone has a retention gate it must pass before the next one starts. This document is the project's review: the evidence first, then the roadmap.

*Evidence convention: claims about the code cite `path:line` in the repo at commit `13c9803` (master). That code is the primary source, so these claims have no URL. External claims carry inline links. No Gradle build or device test was run, so rendering, performance and perceived audio/haptic quality are unverified.*

## Scorecard: polished surface, thin depth, fragile foundations

The table combines the UI/UX audit scores with the novelty, engineering and market assessments. UI sub-scores come from code-level evidence. The engineering and popularity grades are this review's synthesis of the ranked risks and the forecast models below.

| Dimension | Score (1–10) | One-line verdict |
|---|---|---|
| Onboarding / first run | 5 | Guest path exists and migrates data, but guests see Login on every cold start; no splash or tutorial board |
| Core controls | 7 | Tap/long-press, Dig/Flag switch, chording, safe first click, confirm dialogs; no zoom, no chord in flag mode |
| Game depth / modes | 3 | Three 9×9 presets; no custom, no-guess or daily mode |
| Hearts / economy UX | 5 | Clearly communicated, but 1-hour regen is harsh and it needs the network |
| Visual identity | 7 | Cohesive ocean/teal art, contrast-checked numbers; hidden tiles barely contrast with revealed ones |
| Game feel | 6 | Rich haptics, confetti, best-time chip; DTMF "telephone" beeps; no reveal cascade or explosion |
| Accessibility | 6 | Real TalkBack cell labels; cells about 38dp; tile-state contrast fails |
| Adaptive layout / insets | 3 | Top cutout only; landscape unplayable; no tablet layout |
| Localisation | 1 | Effectively all strings hardcoded |
| Navigation / IA | 6 | Shared top bar and back handling fixed; placeholder tab in the nav; icon-only nav |
| **UI/UX average** | **≈4.9** | |
| Game ingenuity / novelty | 3 | Classic rules plus standard F2P scaffolding; only the presentation is distinctive |
| Engineering / launch readiness | ≈4 | Core loop works end to end; 5 launch blockers; near-zero tests |
| Popularity potential (as is) | ≈3 | Commodity genre, colliding name, energy gate; organic ceiling in the tens of thousands |

The pattern is consistent across every audit: **visible UI polish is high, and the debt is structural**. The team's own `doc.md` audits are accurate about the visible bugs (all 18 second-audit items check out in code). But `doc.md` never covers backend integrity, crashes, economy balance or release hygiene. Those areas are where the real launch risk is.

## UI/UX: the board is well crafted, the product decisions around it are not

### First run and offline play lose players before the first tap

There is a guest flow ("Continue as guest", one tap to Home, with the note "Guest progress is saved on this device only"), and guest data is migrated on sign-in. That's good practice (`LoginScreen.kt:138-170, 194`; `MainActivity.kt:584-640`). But `UserSessionStore` persists only a signed-in profile and has no guest flag (`UserSessionStore.kt:14-29`). The guest tap is `screen = Screen.Home` and saves nothing (`MainActivity.kt:669-671`). So **a guest sees the Login screen on every cold start**. For signed-in players the problem is worse. Starting a game spends a heart through a Firestore transaction (`FirestoreLifeRepository.kt:48-59, 315-327`), transactions need the server, and so **a signed-in player who is offline cannot start a single-player puzzle game**. They see "Couldn't start the game. Check your connection" (`MinesweeperViewModel.kt:345-361`). Benchmark apps advertise the opposite. The F-Droid Minesweeper needs no network and has no ads, analytics or accounts, and it offers custom boards, no-guess mode and saved games ([F-Droid](https://f-droid.org/de/packages/com.joeld.minesweeper/)). Minesweeper Classic on Play is sold as offline-capable ([AppGoblin](https://appgoblin.info/apps/com.inspiredsquare.minesweeper)).

### Controls are solid; board size and zoom are the gap

Tap reveals and long-press always flags (`GameScreen.kt:1111-1117`). A labelled, radio-role Dig/Flag segment outlines the board in red in flag mode (`GameScreen.kt:487-541, 941-960`). Tapping a satisfied number chords, matching desktop convention ([Minesweeper Blast guide](https://minesweeperblast.pages.dev/minesweeper-mobile)). The first click always opens an area, paused boards are covered and hidden from screen readers, and the game auto-pauses on background (`GameScreen.kt:971, 999-1053`; `MainActivity.kt:183-187`). The misses are cheap to list and costly to leave:

- In flag mode a tap on a number toggles a flag instead of chording (`GameScreen.kt:264-269`).
- The timer starts in `beginGame()` before the first reveal (`MinesweeperViewModel.kt:364-385`).
- After 30 minutes the game silently returns to Home and the heart is lost (`MinesweeperViewModel.kt:62, 432-435`).
- Flag mode resets on rotation (`GameScreen.kt:153`).
- There's no zoom/pan: the grid is a fixed, non-scrolling `LazyVerticalGrid` (`GameScreen.kt:963-966`).
- There's no save/continue, undo, question-mark state or long-press-duration setting.

Antimine ships Beginner through Legend plus custom boards, four control styles, custom long-press duration, an adjustable touch area, "continue after clicking a mine", saved games and dynamic/AMOLED themes ([Antimine README](https://github.com/lucasnlm/antimine-android)). Zoom that doesn't snap is a recurring request in App Store reviews ([Mineswifter](https://apps.apple.com/us/app/id1521190195); [Minesweeper Classic: Retro](https://apps.apple.com/gb/app/id1287818410?l=en-gb)).

### Hearts gate a genre that players expect to be free

Every game costs a heart up front and a win refunds it. There are 5 hearts, 1 regenerates per hour, and a refill costs 5 diamonds per heart (`LifeRules.kt:5-19`; `MinesweeperViewModel.kt:337-344, 556-560`). The communication is good: "−1 ♥" on Start, "Out of hearts · Refill" at zero, countdowns and accessible labels (`HomeScreen.kt:489-570`; `HeartsIndicator.kt:106-130`). Two problems remain. The Game screen never shows the heart count, and a skilled player on a bad run of five losses waits up to five hours. The leading free apps have no lives system at all ([Antimine](https://github.com/lucasnlm/antimine-android); [F-Droid](https://f-droid.org/de/packages/com.joeld.minesweeper/)). Player sentiment on energy systems in puzzle games is anecdotal but consistent. Reviews of one mystery-puzzle app were dominated by complaints about its energy system ([Marlvel](https://marlvel.ai/apps/com-artifexmundi-unsolved/reviews)). An itch.io player found energy "more annoying" because one mistake ran it out ([itch.io](https://itch.io/post/3708783)). Designers advise systems that reward success rather than punish failure ([Adrian Crook](https://adriancrook.com/?p=8174)). The steelman case also exists: energy teaches session length and encourages daily return ([Pocket Gamer](https://www.pocketgamer.com/harry-potter-hogwarts-mystery/making-the-case-for-harry-potter-hogwarts-mysterys-energy-system/)). In Minesweeper, though, losses on a guess-prone board come down to luck, so **charging a heart for a forced 50/50 erodes trust**, and no controlled study shows the opposite.

### Visual identity is the real asset, with fixable contrast and motion issues

The identity is better than typical clones: a teal brand, a Deep Ocean variant, animated ambient scenes, hand-drawn flag and mine art, and two number palettes commented as WCAG AA (`Color.kt:49-80`) and chosen by cell luminance (`GameScreen.kt:1174-1177`). Digits are always drawn, so colour is never the only cue. That is the "double-coding" principle in both the Xbox guidelines and Ian Hamilton's work ([XAG 103](https://learn.microsoft.com/en-gb/gaming/accessibility/xbox-accessibility-guidelines/103); [Colorblind Games](https://colorblindgames.com/2021/08/02/colorblind-gaming-101-some-solutions)). The weak point is the board itself. Hidden-vs-revealed tile contrast, computed from the tokens, is **1.31:1 in Classic light, 1.35:1 in Ocean light, 1.70:1 in Ocean dark and 2.35:1 in Classic dark**. All are below the 3:1 non-text threshold (`Color.kt:20-38, 103-126`). White on the gold best-time chip is 2.38:1, against the 4.5:1 that small text needs ([Android accessibility](https://developer.android.com/guide/topics/ui/accessibility/apps)). Light-theme teal on the background is 2.66:1. An animated fish-and-rays scene runs behind the board during play (`GameSceneBackground.kt:56, 195, 232, 308-315`). Immersive mode hides the clock and battery on every screen, not just the game (`MainActivity.kt:151-161`). The store sells only 4 items (`StoreCatalog.kt:16-21`).

### Game feel, accessibility and adaptivity

Feedback is layered: distinct haptic waveforms for tap, flag, explosion, win, best time, level-up and achievement, plus confetti and a pulsing best-time chip. But all audio is `ToneGenerator` DTMF digit runs on the music stream (`GameFeedback.kt:33-36, 120-140`), which sounds like a telephone, not a game. Flood fill reveals instantly with no cascade, and the explosion is a static drawing (`GameScreen.kt:1087-1091, 1151-1155`). ReduceMotion reads only the system animator scale, once, and there's no in-app setting (`ReduceMotion.kt:17-26`).

For TalkBack, each cell announces "Row r, column c, state" with custom click labels (`GameScreen.kt:1121-1124, 1196-1205`). There are no grid collection semantics and no live region for results. Touch targets fall short of Android's 48dp: cells are about **37.9dp** on a 411dp phone, the Home pills are 44dp and the Dig/Flag segments about 34dp. They do pass WCAG 2.2's 24px minimum ([Android](https://developer.android.com/guide/topics/ui/accessibility/apps); [W3C SC 2.5.8](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html)). The bottom nav is icon-only, and one of its four slots is the placeholder More Games, while Google's guide shows labels and reserves the bar for destinations of equal importance ([Android navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar)).

Adaptivity is the weakest area. Targeting SDK 37 means Android ignores orientation and resizability limits on displays of 600dp and wider unless the app declares `android:appCategory="game"`, and the manifest doesn't ([Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)). The repo has no `WindowSizeClass` code ([window size classes](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)). Landscape on a phone leaves an estimated 150dp for the board, about 14dp cells, which is unplayable. Localisation hasn't started: `strings.xml` holds two real strings, and only `MainActivity.kt` uses `stringResource`.

## Ingenuity: a 3/10 that one rule twist and one social loop could double

Mines' design pillars are a faithful classic core with standard free-to-play retention around it: hearts, coins/diamonds, cosmetics, 5×5 achievement tiers and a "THE WINTER CUP … COMING SOON" tournament teaser (`TournamentScreen.kt:141-230`). Every mechanic has a direct precedent. minesweeper.now already runs daily and weekly quests, login streaks, chests, trophies and a ticket-gated same-board PvP Arena ([minesweeper.now Arena](https://minesweeper.now/help/arena)). minesweeper.online runs seasonal events, quests and ranked PvP ([profile](https://minesweeper.online/player/19176606)). No-guess boards come free with Mines+, Antimine and "The Clean One" ([Product Hunt](https://www.producthunt.com/p/mines-plus/mines-3); [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/dev.lucanlm.antimine?repo=main); [AppFollow](https://apps.appfollow.io/android/minesweeper-the-clean-one/ee.dustland.android.minesweeper?country=ru)). The rubric gives core mechanics 1/3, meta 1/3, presentation 1–1.5/2 and social 0/2 (not shipped), **about 3/10**. Mines is also *narrower* than its rivals: dense rather than big, and guess-prone.

The competitive landscape splits sharply. On Play, the leaders are near-identical classics rated 4.2–4.8 that compete on polish and staying ad-light: Evkar 4.8, Dustland "The Clean One" 4.7 (guess-free, offline, minimal), Infinità 4.2 ([Play search](https://play.google.com/store/search?q=minesweeper&c=apps)). Minesweeper Pro has about 1.3M installs at 4.72 stars ([AppGoblin](https://appgoblin.info/apps/mindware.minegamespro)), and Minesweeper Classic: Retro has 1M+ ([APKCombo](https://apkcombo.com/minesweeper-classic-retro/com.still57.minesweeper/)). Genuine innovation lives on PC and web, where it earns acclaim more readily than scale:

| Title | What changed | Reception | Source |
|---|---|---|---|
| 14 Minesweeper Variants (2022) | 14 rule variants, every board solvable by logic | ~97% positive; est. 48K–112K copies, $253K–$281K (sources conflict) | [GameRevenueData](https://gamerevenuedata.pages.dev/games/14-minesweeper-variants); [VaporLens](https://vaporlens.app/app/1865060/14_minesweeper_variants) |
| 14MV 2 (2024) | 14 new variants | 96% positive of 264–347 reviews | [Steam](https://store.steampowered.com/app/2631960/) |
| Tametsi | Hand-crafted zero-guess puzzles, mixed geometry | ~97% positive | [Buried Treasure](https://buried-treasure.org/2020/02/tametsi/) |
| Dragonsweeper (2025) | RPG hybrid: monsters with HP, levelling | Critical buzz ("mildly obsessed") | [waxy.org](https://waxy.org/2025/02/kyle-orland-on-the-genius-of-dragonsweeper) |
| Mamono Sweeper | Numbers sum adjacent monster levels; HP replaces instant death | Cult classic | [mzrg](https://mzrg.com/mines/mamono.shtml) |
| Minesweeper Genius | Minesweeper × Picross with a story | OpenCritic 75; "levels feel random" | [OpenCritic](https://opencritic.com/game/7464/view/reviews) |
| Dragonsweeper-likes (2025–26) | Dungeon/roguelite reskins | Small: 13–28 reviews | [Playpile](https://playpile.gg/games/minesweeper-dungeon-rpg); [Gematsu](https://www.gematsu.com/2026/05/fantasy-globetrotting-minesweeper-game-dungeon-sweeper-plus-announced-for-pc) |

The white space for Mines comes in three layers. The first is **table stakes**: no-guess generation and bigger boards. Without them, a forced guess can cost a heart. The second is a **cheap, proven habit loop**: a daily seeded puzzle. LinkedIn's daily games report **84% next-day return**, built on streaks and a fixed midnight reset ([Engadget](https://www.engadget.com/linkedin-now-has-wordle-style-games-you-can-play-every-day-133035046.html); [LinkedIn Help](https://www.linkedin.com/help/linkedin/answer/a6863543)). The third is a **signature differentiator** that turns the ocean art into mechanics. An "Abyss" dive mode in the Mamono/Dragonsweeper mould would use depth layers as boards, sea creatures with levels instead of binary mines, and oxygen as a resource. Polished entries look sparse on mobile, but this wasn't verified because store-data tools were unavailable. Variant-rule packs are the alternative, as long as they come with clear hints, since obtuse hints are 14MV's main complaint ([VaporLens](https://vaporlens.app/app/1865060/14_minesweeper_variants)). Doing one rule twist and one social loop would plausibly lift novelty to 6–7.

## Engineering: a working loop on an untrustworthy, crash-prone foundation

### What is built and what is placeholder

The codebase is about 15.7K lines of Kotlin across `app`, `core:{theme,ui,session}` and 13 feature modules. The full loop works end to end: Google or guest login, Home, Game, result, Celebration, History/Profile/Achievements, Store, Hearts and Settings, including account deletion with re-authentication (`GoogleAuthManager.kt:256-314`). Several things are partial or placeholder:

- **Tournament** and **More Games** are teasers, and More Games occupies a nav slot.
- **Wallet** is data-only with no UI.
- The **Robo avatar does nothing once bought**, and **Frost Cells** only thickens the cell border (`MinesweeperViewModel.kt:326-329`; `GameScreen.kt:1107-1108`). That's a refund and review risk the moment diamonds become purchasable.
- **Feedback screenshots** are counted but never uploaded (`MinesweeperViewModel.kt:726-733`).
- **Analytics** is linked but logs zero events: a grep for `logEvent` finds nothing.
- **Daily reward, onboarding, leaderboards, hint/undo, Continue game and localisation** are absent.

### The engine is correct but not seedable, and the timer can be gamed

The engine is small and mostly right: lazy mine placement excluding the first cell and its neighbours, BFS flood fill, standard chording, and a win when all safe cells are revealed (`MinesweeperGame.kt:37-60, 149-244, 276-281`). There are four defects:

1. **First-click safety edge case.** `reveal()` places mines *before* checking `isFlagged`, and flagging is allowed before the first reveal. Tapping a pre-flagged cell therefore places the mines, and the player's real first tap is no longer guaranteed safe (`MinesweeperGame.kt:125-133, 246-263`).
2. **No seed.** `candidates.shuffled(Random)` is unseeded (`:52`), so daily puzzles, fair tournaments, replays and server validation are all impossible today.
3. **Tick-counting timer.** It increments on `delay(1.seconds)`, and pausing drops the partial second. **Rapid pause/resume freezes the clock**, which is an exploit for best times and for any future leaderboard (`MinesweeperViewModel.kt:393-445`).
4. **Skewed XP.** Every difficulty gives the same XP (wins×100 + losses×10), so spamming Easy is the fastest way to level (`LevelSystem.kt:2-5`).

The only real tests are 14 cases in `LifeCalculatorTest.kt`. `MinesweeperGame`, `ScoreCalculator`, `LevelCalculator` and `AchievementCalculator` have none.

### The economy is client-owned and too tight to be fun

Coins are earned only on wins (50/100/200 by difficulty). Diamonds come only from redeeming 500 coins, at most 2 per rolling 24 hours (`CoinWalletDataStore.kt:38-40`). A heart refill costs 5 diamonds, **2.5 days of maximum redemptions, to save an hour of regen**, so refills are almost never worth it. The full catalog costs 45 diamonds, which takes at least 23 days and 450 Easy wins, and after that there is nothing left to spend on. Integrity is worse:

- Clients write balances directly: `FieldValue.increment` on coins and diamonds, `arrayUnion` on inventory, and direct writes of hearts (`FirestoreWalletRepository.kt:337-353`; `FirestoreLifeRepository.kt:340-345`).
- **There is no `firestore.rules` or `firebase.json` in the repo**, and `google-services.json` is committed.
- Heart regen and the redeem cap use the device clock even for signed-in users, so setting the clock forward grants hearts instantly.
- Purchases aren't atomic: diamonds are spent in one transaction, ownership is written separately, and the refund sits in a catch block (`MinesweeperViewModel.kt:295-317`).
- `allowBackup="true"` with template rules backs up the plain-DataStore guest economy.

Whether the live console rules are locked down is **unknown**, and that alone is a launch-blocking question.

### Sync, crashes and architecture

Repositories route each call to Room/DataStore or Firestore depending on whether a Firebase user exists. Guest-to-account migration runs only on an account's *first* sign-in. The user document is created *before* migration (`FirestoreGameRepository.kt:61`), so **the retry promised in comments ("it'll be retried on the next sign-in attempt") can never happen** (`MainActivity.kt:595-598`). Partial uploads duplicate or orphan games, and hearts aren't migrated. Signed-in history is `limit(100)` (`FirestoreGameRepository.kt:130-139`), and level, achievements and celebrations are computed from that list. After 100 games a player's **level freezes or drops**, and the Games Played and Games Won DIAMOND tiers become **unreachable** (`AchievementCalculator.kt:62-81`). Firestore calls in `completeLevel`, `redeemDiamond`, the sign-in follow-ups and the history loads have no try/catch, and snapshot listeners call `close(error)` into collectors that don't catch it. With no Crashlytics and no logging, **network errors crash the app silently**.

Architecturally, one 830-line `AndroidViewModel` constructs every repository, and MainActivity builds a second set. There's no DI framework. About 25 loose `MutableStateFlow`s hold state, migration and deletion logic live in composable lambdas, and `api(...)` leaks Firebase and Room across 12 module build files. Game state doesn't survive process death, and the Game branch mutates `screen` during composition (`MainActivity.kt:995-998`). Both README "next steps" (api→implementation, split the ViewModel) are still open.

### Ranked risks

| # | Risk | Effort | Blocks |
|---|---|---|---|
| 1 | No Firestore rules in repo; client-authoritative economy and stats | M rules / L Functions | Launch |
| 2 | Uncaught Firestore/network exceptions crash the app | S–M | Launch |
| 3 | No crash reporting or logging | S | Launch |
| 4 | R8 off, template keep rules, no signing config, versionCode 1 | S–M | Launch |
| 5 | No privacy policy or analytics consent; deletion leaves feedback PII, deletes data before the Auth user | S–M | Launch |
| 6 | 100-game cap breaks level and achievements | M | Soft launch |
| 7 | Device-clock trust in regen and redeem cap | M | Leaderboards / IAP |
| 8 | Signed-in users can't start offline; no result outbox | M | Soft launch |
| 9 | Migration retry impossible; no merge; hearts not migrated | M | Soft launch |
| 10 | Game lost on process death and at the 30-minute auto-quit | M | Soft launch |
| 11 | Pause/resume timer exploit; clock starts before first click | S | Leaderboards |
| 12 | Unrestricted backup of guest economy and session cache | S | Launch |
| 13 | Tight economy; dead Robo / near-invisible Frost purchases | S hide / M art | IAP |
| 14 | No seed or no-guess; 9×9 only; first-click-on-flag bug | S seed / L solver | Daily / tournaments |
| 15 | God ViewModel, manual DI, `api` leaks, one test class, Room `exportSchema=false` | L (M for engine tests) | Velocity |

Effort: S ≤1 day, M = days, L = 1+ week. These are judgement estimates, not measurements.

## Popularity: an evergreen niche with a low ceiling and a name problem

Puzzle is the largest and only growing top casual genre, at **$8.2B in 2025 (+7.6% YoY)**. But match-3 alone takes $4.8B (about 59%), and logic or Minesweeper titles aren't even broken out ([Mobidictum / AppMagic](https://mobidictum.com/?p=210271)). The pool that matters to Mines is the ad-monetised classic-logic long tail. The largest confirmed Minesweeper listing, Panu Vuorinen's, has **5M+ downloads** ([Google Play](https://play.google.com/store/apps/details?id=com.panu&hl=en_US)). Hyperlite's Classic Edition has 500K+ ([Google Play](https://play.google.com/store/apps/details?id=com.hyperlite.minesweeper&hl=en)) and ssaurel's Classic 100K+ ([AppBrain](https://www.appbrain.com/app/minesweeper-classic/com.ssaurel.minesweeper)). ABCTech's clone has about 35K installs, is 100% ad-monetised and earns under $10K/month, AppGoblin's lowest bucket ([AppGoblin](https://appgoblin.info/apps/com.raymond.minesweeper)). Much of the demand is served on the web: minesweeper.online drew about 2.9M visits in April 2025 ([Ahrefs](https://ahrefs.com/websites/minesweeper.online)).

**The name "Mines" is a liability.** It's the name of a Stake Original crypto-casino game and many clones ([win.gg](https://win.gg/crypto-casinos/mines/)), and searches for "Mines game" return casino affiliate pages ([sportsgrid](https://www.sportsgrid.com/social-casinos/stake-us-mines-strategy)). Players type "minesweeper". A coins-and-mines theme can also look gambling-adjacent to reviewers. The store title should be "<Brand>: Minesweeper Puzzle", for example "Deep Sweep", "Sonar Sweep" or "Tidal Mines", with no bet, cash-out or multiplier language anywhere. The package `com.genoma.mines` can stay.

Benchmarks frame what success looks like. GameAnalytics medians are **D1 ≈22%, D7 ≈4%, D30 ≈0.7%**, and the top 10% reach 40% / 12% / 4% ([GameDev Reports](https://gamedevreports.substack.com/p/gameanalytics-mobile-and-pc-game)). Puzzle had the best D7 of any genre in Q1 2024 ([GameDev Reports](https://gamedevreports.substack.com/p/gameanalytics-benchmarks-in-mobile)). Android rewarded eCPM is about $9 in North America but $1.8–5 in other regions, and banners pay about $0.5 ([Playio / Appodeal](https://blog.playio.co/mobile-game-ecpm-benchmarks-2026)). Android eCPMs were near three-year lows in H1 2025 ([TopOn](https://mores.toponad.com/reports/TopOn%20Global%20Mobile%20Games%20Monetization%20Report%20_%202025%20H1.pdf)). US casual Android CPI is $1.50–3.50 ([Playio](https://blog.playio.co/mobile-game-cpi-benchmarks-2026)). Against a realistic **$0.02–0.06 ARPDAU**, paid acquisition can't be ROI-positive, so growth must be organic. The forecast below is illustrative arithmetic built on those inputs, not a sourced prediction:

| Scenario | 12-mo installs | D1 / D7 / D30 | Avg DAU (peak) | Blended ARPDAU | 12-mo gross |
|---|---|---|---|---|---|
| Pessimistic | 2,000 | 15% / 3% / 0.7% | ~10 (~50) | $0.01 | < $100 |
| Base | 20,000 | 28% / 8% / 3% | ~330 (~800) | $0.03 | ~$3–5K |
| Optimistic (featuring or viral moment) | 250,000 | 35% / 12% / 5% | ~6,800 (~15K) | $0.05 | ~$100–140K |

Indie outcomes span from GravityShot's roughly 50 downloads ([postmortem](https://gamedo-live.pages.dev/news/gravityshot-postmortem-shipping-mobile-game-godot)) to Starlight X-2's 9,970 in six months ([Game Developer](https://gamedeveloper.com/business/revenue-expenses-and-statistics-of-starlight-x-2-a-mobile-puzzle-game)). TikTok turned Storyteller around a year after launch ([GamesRadar](https://gamesradar.com/games/puzzle/indie-dev-thought-their-puzzle-game-would-just-die-out-but-a-weird-sales-curve-and-tiktok-virality-led-to-great-success-a-year-after-launch)), but success stories like that are survivorship-heavy. If three developers value their time at even $1K/month each, **only the optimistic case covers costs**. Mines should be run as a portfolio or learning title until soft-launch retention beats the top quartile.

The best-fit monetization is **ad-first hybrid**. Rewarded ads give +1 heart, a "second chance", doubled coins or a hint. Interstitials run after every 2–3 completed games, never mid-game. A "Remove Ads" IAP at $2.99–4.99 keeps the optional rewarded ads, and cosmetic bundles and coin packs round it out. Banners should be skipped to protect the "clean" positioning that top-rated rivals use. Premium and subscription models don't fit a genre where competitors are free. Two Play policies constrain the design. Non-gambling apps may not let users participate "using real money (including in-app items purchased with money) to obtain a prize of real world monetary value". Purchase-linked loyalty rewards must use fixed, disclosed ratios and may not be "exponentiated by game performance or chance" ([Play Policy 9877032](https://support.google.com/googleplay/android-developer/answer/9877032)). So tournaments must be free or entered with earned-only coins, with cosmetic-only prizes.

## Roadmap: six gated milestones over roughly twelve months

The sequence follows the dependency chain in the evidence. **Stability and integrity come first**: crashes and editable balances make every later metric meaningless. **Fairness comes before retention**: seeded no-guess boards make streaks and tournaments trustworthy. **Retention comes before monetization and social**: a gate stops growth spend on a leaky loop. Several Play requirements are already met. targetSdk 37 clears the API 36 bar in force since Aug 31 2026 ([Android Developers](https://developer.android.com/google/play/requirements/target-sdk)), and in-app deletion exists. The process obligations remain:

- A personal account created after Nov 13 2023 needs **12+ testers opted in for 14+ continuous days** ([Play Console Help](https://support.google.com/googleplay/android-developer/answer/14151465)).
- A **web deletion link** must be declared in Data safety ([Play Console Help](https://support.google.com/googleplay/android-developer/answer/13327111)).
- A privacy policy, an IARC rating and an accurate Data safety form are also required.

The team's account type and creation date are unknown, so the 12-tester rule should be assumed. The app has no public listing yet ([Play URL returns 404](https://play.google.com/store/apps/details?id=com.genoma.mines)).

The suggested team split (synthesis) is one engineer on client and generator, one on backend, Functions and analytics, and one on design, art, community and the content calendar. Art is the bottleneck, so each season should reuse one art set across backdrop, skin and avatar.

| Milestone | Window (from 6 Nov 2026) | Goal | Exit gate |
|---|---|---|---|
| **M0 Foundation** | Weeks 0–4 (to early Dec 2026) | Stable, compliant, measurable, cheat-resistant baseline | No P0 crashes; rules in repo and deployed; Play declarations complete |
| **M1 Fair Board + Closed Test** | Weeks 4–9 (Dec 2026–mid Jan 2027) | Every loss is the player's fault; mandatory closed test | 14-day closed test passed; production access granted |
| **M2 Daily Tide + Soft Launch** | Weeks 9–18 (mid Jan–mid Mar 2027) | Habit loop proven on real cohorts | D1 ≥35%, D7 ≥12%, D30 ≥5% |
| **M3 Global Launch + Monetization** | Weeks 18–24 (mid Mar–late Apr 2027) | Ship worldwide with ad-first hybrid revenue | Vitals clean; rating ≥4.3; D7 holds after ads |
| **M4 Winter Cup + Social** | Months 6–9 (May–Aug 2027) | Fill the Tournament placeholder with fair, validated competition | Cheat rate low; D30 ≥5%; organic share loop live |
| **M5 Signature + Expansion** | Months 9–12+ (Sep 2027 →) | Raise novelty to 6–7; extend reach | Go/no-go on D30 and DAU before iOS/web spend |

### M0 Foundation: stop the crashes, lock the economy, start counting (weeks 0–4)

**Scope.** Stability first:

- Add a `CoroutineExceptionHandler`, try/catch around every Firestore call, and `.catch {}` on listener flows.
- Add Crashlytics and minimal logging.
- Turn on R8 with verified Firebase/Room keep rules, set up Play App Signing, and add a versionCode scheme.

Integrity:

- Write `firestore.rules` into the repo and deploy them with field-level validation that blocks direct balance writes beyond bounded increments.
- Review the unauthenticated `feedback` create rule.
- Switch timestamps to server time.
- Restrict the backup rules.

Correctness:

- Fix the flag-before-first-reveal safety bug.
- Replace the tick timer with an elapsed-realtime delta that starts on first reveal.
- Remove the silent 30-minute quit.
- Make purchases atomic in a single transaction.

Compliance:

- Publish a privacy policy and link it in Settings.
- Add a web deletion URL, delete feedback PII on account deletion, and delete Auth after re-auth succeeds.
- Add analytics consent (UMP if ads are coming) and complete Data safety and IARC (answering the simulated-gambling question honestly).

Product quick fixes:

- Persist "guest chosen" and skip Login.
- Hide the More Games and Tournament tabs, or turn Tournament into a "Notify me" demand signal.
- Hide Robo and Frost until their art exists.
- Add `android:appCategory="game"`.
- Raise hidden-tile contrast to ≥3:1 and enlarge the pills and segments to 48dp.
- Rename the listing to "<Brand>: Minesweeper Puzzle".

Measurement: an `AnalyticsTracker` interface in `core:session` or a new `core:analytics`, covering `game_start`, `first_click`, `game_end` {result, difficulty, mode, duration_s, cause}, `hearts_depleted`, `heart_refill`, `earn/spend_virtual_currency`, `store_view`, `item_purchase`, `achievement_unlocked` and `level_up`. Add unit tests for `MinesweeperGame`, `ScoreCalculator`, `LevelCalculator` and `AchievementCalculator`.

**Exit criteria.** Internal track crash-free sessions ≥99.5%. Pre-launch report clean. Rules versioned and deployed. Every Play Console declaration complete. Events visible in DebugView. Engine and calculator tests green in CI.

### M1 Fair board and closed test: make losses feel earned (weeks 4–9)

**Scope.** Make the generator seedable (seed + first click → board) in `feature:game`. That's the single prerequisite for daily puzzles, challenges, tournaments and server replay validation. Ship **no-guess mode** using regenerate-and-test with a single-point plus subset-rule solver, the approach minesweeper.now uses ([FAQ](https://minesweeper.now/faq/no-guessing-minesweeper)). Defer Simon Tatham's perturbation method, which handles densities over twice the Windows standard ([Tatham](https://www.chiark.greenend.org.uk/~sgtatham/quasiblog/mines-solver/)), to expert boards later.

Restore the skill ceiling with Intermediate 16×16 and Expert 30×16, plus pinch-zoom/pan, an adjustable touch area and a long-press setting. That also fixes the 48dp touch-target problem on large boards.

**Rethink hearts.** Classic play becomes unlimited, and hearts gate only Hard or ranked modes or act as a "second chance" continue. Put the regen interval, `DIAMONDS_PER_HEART` and reward amounts in Remote Config, which allows tuning without a release ([Firebase](https://firebase.google.com/products/remote-config)). Rebalance the economy so diamonds have a reasonable sink.

Small fixes: allow chording in flag mode, make flag mode saveable, and show remaining hearts on the result banner. Make hearts local-first so signed-in players can start offline, and add a result outbox. Add In-App Review gated on, for example, 3+ wins over 2+ sessions with no recent loss, never on a button ([Android Developers](https://developer.android.com/guide/playcore/in-app-review)).

Run the **closed test**: 12–30 engaged testers recruited from friends, r/AndroidClosedTesting and puzzle Discords, for at least 14 days. Rejections for low engagement are reported ([DEV](https://dev.to/testerscommunity/google-play-rejected-my-app-after-14-days-of-testing-here-is-what-i-did-wrong-3c21)). Collect qualitative feedback on the first-run experience, hearts pacing and the store.

**Exit criteria.** 14 consecutive days with ≥12 opted-in testers. Zero P0 bugs. Production access granted. Solver-verified no-guess generation at ≤ about 200ms on a mid-range device (a target to set, not a benchmark). Tester feedback shows no "unfair loss" theme.

### M2 Daily Tide and soft launch: prove the habit loop (weeks 9–18)

**Scope.** Ship **"Tide of the Day"**: one date-seeded no-guess board per tier, the same worldwide, with a streak that counts *playing*, not winning. That follows Duolingo's lesson from 600+ streak experiments that retention is "most fragile in the first seven days" and that the streak should be simple ([Lenny's Newsletter](https://www.lennysnewsletter.com/p/behind-the-product-duolingo-streaks)). Add one free streak freeze a week and a spoiler-free emoji share card.

Data correctness:

- Detailed stats: win rate per difficulty, best and average times, 3BV/s, current and longest streak.
- Fix the 100-game cap by storing XP and achievement counters on the user document.
- Make migration idempotent with per-game IDs and a migration-state flag, and migrate hearts.
- Save/continue the board and seed so games survive process death.

Backend: start **Cloud Functions** (`grantReward`, `spendDiamonds`, `consumeHeart`, `submitDailyScore` with move-log replay against the seed), and put App Check in monitor mode before enforcing ([Firebase](https://firebase.google.com/docs/app-check/android)). App Check proves the request comes from a genuine app, not that the player is honest, so server validation is still required.

Feel: SoundPool SFX with game audio attributes in place of the DTMF beeps, a ReduceMotion-gated reveal cascade, a loss shake, an in-app reduce-motion setting and a calm-board toggle. Add a compact-height landscape layout.

**Soft launch** as a country-limited production release in 1–3 markets (for example Philippines, Canada, Australia/NZ) for 4–6 weeks, with a $300–1,000 UA budget to get 1,000+ installs per cohort. Iterate weekly.

**Exit criteria (KPI gate).** **D1 ≥35%, D7 ≥12–15%, D30 ≥5%** (soft-launch practitioner targets, [Lancaric](https://lancaric.substack.com/p/the-soft-launch-bible-full-2025-blueprint)). Average session ≥5 minutes and ≥2 sessions/day. Store-listing conversion ≥25–30% via listing experiments. Daily participation tracked, and 7-day streak survival measured. Crash-free ≥99.5%. **Missing the gate after two iterations means rethinking the core loop or meta before going global.**

### M3 Global launch and monetization: earn without breaking trust (weeks 18–24)

**Scope.** AdMob (or MAX/LevelPlay mediation) with rewarded placements: +1 heart, second chance, double coins, hint. Add a capped interstitial every 2–3 completed games, a Remove Ads IAP, and cosmetic bundles via a current Play Billing Library. Update the ads declaration and Data safety. Add Play Games Services v2 (auto sign-in, leaderboards per difficulty and for the daily, mirrored achievements; [Android Developers](https://developer.android.com/games/pgs/signin)), FCM opt-in after the first daily win (streak-at-risk, daily-ready, hearts-full, with frequency caps) and flexible In-App Updates.

Run the first A/B tests in this order: heart regen 60 vs 30 vs 20 minutes; first session on a no-guess Beginner board vs the difficulty picker; one free freeze per week vs none. With indie volumes, prefer a few tests with large effect sizes. Run extract-strings localisation for the top soft-launch locales. Go global with a featuring nomination, and start short-form video at 3–5 clips a week (cascade clears, "can you solve this?" boards, ocean theme showcases, devlog).

**Exit criteria.** Android Vitals below the bad-behaviour thresholds (ANR <0.47%, crash <1.09%; the thresholds were unverified this session). Rating ≥4.3. D7 within 1 point of the pre-ads cohort. Blended ARPDAU ≥$0.02 and tracked. No policy strikes.

### M4 Winter Cup and social: competition only once results are trustworthy (months 6–9)

**Scope.** Async friend challenges via seed App Links (Dynamic Links is deprecated). Then the **Winter Cup** as a weekly same-seed async tournament: server-validated replay, time-boxed leaderboards, cosmetic-only prizes, and free or earned-coin entry so it stays inside Play policy 9877032. Rename it seasonally, since a "Winter Cup" in mid-2027 reads oddly. Add weekly "Tide Challenge" modifiers (fog of war, time attack, no-flag, a dense "Kraken" board) from a Remote Config calendar, with a 2-season content buffer, scheduling at least 2 weeks ahead and a kill switch per event. Add ocean seasons: "Dive depth" levels mapped onto `LevelSystem`, a creature album for streak milestones at 7/30/100 days, and a free "Current Pass". Add a Glance widget for today's daily and the streak. Finish the DI migration and ViewModel split, so that velocity holds up under LiveOps.

**Exit criteria.** Rejected-score rate from replay validation tracked and below an agreed threshold. Tournament participation ≥15% of weekly actives (a target to set). D30 ≥5% sustained. Measurable installs attributed to shares and challenges.

### M5 Signature differentiator and platform expansion (months 9–12+)

**Scope.** Pick **one** signature bet: the "Abyss" ocean-dive roguelite (sea creatures with levels or HP, oxygen, relics, about 2–4 months) or variant-rule packs (about 1–2 weeks each on a shared solver, with clear hints). Extract `:core:engine` as a KMP module (game, score, level, life and achievement rules). It feeds the Functions validator and unlocks iOS via Compose Multiplatform, which has been stable for iOS since 1.8.0 in May 2025 ([JetBrains](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/)). Add a web daily-puzzle page for shareable links and SEO, after checking whether the web target has matured past Alpha. Add Google Play Games on PC through the same PGS integration ([Android Developers](https://developer.android.com/games/playgames/native-pc/pgs)). Clubs or friend leagues wait for a meaningful DAU base.

**Exit criteria.** Go only if D30 ≥5% and DAU are trending up over 8 weeks. Measure the signature mode by its retention lift among adopters and its Day-1 adoption.

## Conclusion

What limits Mines is not code quality in the narrow sense. The Compose craft is real and the visible UI debt is largely paid. The problem is a mismatch between the genre's contract and the product's decisions. Minesweeper players expect fair, unlimited, offline, account-free play. Mines charges a heart for 9×9 boards that often force guesses, shows Login on every guest launch, and needs a server round-trip to start a single-player game. Each of those choices adds friction exactly where the genre's leaders remove it. Turning them around — no-guess boards, unlimited classic play, local-first state — matters more than any new feature.

The seeded generator is the project's keystone. One small engine change unlocks fair losses, the daily habit loop, friend challenges, a legitimate tournament and server-side cheat detection, and it later becomes the shared KMP core for a validator, iOS and web. Honest expectations matter just as much: the realistic outcome is a respected niche title with tens of thousands of installs. Treat the M2 soft-launch retention gate as the real decision point. Clearing top-quartile D7 earns the right to invest in seasons and a signature mechanic, and missing it twice means the core loop needs rethinking before any growth spend.
