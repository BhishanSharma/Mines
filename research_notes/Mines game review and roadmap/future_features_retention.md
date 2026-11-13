# Future Features, Retention and LiveOps Roadmap for "Mines" (as of Nov 2026)

Method note: About 15 tool calls (repo skim plus web search and fetch). Repo facts come from reading C:\Users\hp\AndroidStudioProjects\Mines directly. Web claims carry inline sources. Many search results for NYT/Wordle were SEO spam mirror pages and were excluded. Several retention statistics are available only from secondary sources and are flagged as such.

## Q0. Where does the current codebase stand? (Baseline for mapping features to modules)

### Takeaway
Mines has a polished single-player loop with meta systems already scaffolded: hearts, wallet, store, achievements, and a placeholder tournament. It lacks every piece of LiveOps and retention infrastructure: seeded boards, analytics events, Remote Config, Crashlytics, Play Games, review and update prompts, widgets, notifications, and a server-authoritative economy. Most of the backlog adds to existing modules rather than replacing them.

### Cited Findings (repo, read-only skim, 2026-11-06)
- Modules: `app`, `core:{theme,ui,session}`, `feature:{auth,settings,store,wallet,life,tournament,achievements,game,home,profile,celebration,moregames,userfeedback}` (settings.gradle.kts).
- `gradle/libs.versions.toml`: Kotlin 2.4.21, Firebase BoM 35.1.0 with only `firebase-auth`, `firebase-firestore`, `firebase-analytics`, plus Room 2.8.5 and DataStore 1.3.0. There are no Crashlytics, Remote Config, App Check, Messaging (FCM), Functions, Play Games Services, Play In-App Review, Play In-App Update, or Glance dependencies.
- `feature/game/.../MinesweeperGame.kt`: mines are placed lazily on the first reveal, excluding the first cell and its neighbors (a 3x3 safe opening). Placement uses `candidates.shuffled(Random)`, so it is unseeded and not no-guess. Reveal, chord and flag are implemented.
- `feature/life/.../LifeRules.kt`: `MAX_HEARTS = 5`, `DIAMONDS_PER_HEART = 5`, `REGEN_INTERVAL_MILLIS = 1 hour`.
- `feature/tournament/.../TournamentScreen.kt` (297 lines) is a static "THE WINTER CUP / COMING SOON" screen with a "Play a practice game" button and no backend.
- `feature/achievements/.../AchievementCalculator.kt`: local badge tiers (`BadgeTier.kt`), not wired to Play Games.
- A grep for `logEvent(` across all Kotlin sources returned zero hits. Firebase Analytics is linked but logs no custom events.
- `core/ui/ReduceMotion.kt` exists, so reduced motion is already considered. `contentDescription`/semantics appear in GameScreen, HistoryScreen, HowToPlayScreen and GameSceneBackground.
- wallet, store and life each have `Firestore*Repository` plus `*DataStore`, which means the client writes the coin, diamond and heart balances.

### Inferences
- A seedable, deterministic board generator (seed -> board, including first-click handling) is the single technical prerequisite for daily puzzles, async friend challenges, fair tournaments, replays and server-side score validation. It should be the first engineering item.
- Because analytics has zero events, no retention decision can be data-driven yet. Instrumentation has to come before any A/B test.

### Gaps
- I did not check GameScreen cell rendering for colour-only number encoding or TalkBack cell semantics in depth (the code audit track covers this).

## Q1. Which retention features have evidence behind them?

### Takeaway
The strongest evidence supports daily or seeded content combined with forgiving streaks (NYT Games, Duolingo) and fair no-guess boards, which are a well-known Minesweeper genre differentiator. Play-platform APIs (Play Games Services v2, In-App Review) are cheap wins with official guidance. Widgets, async challenges and real tournaments have weaker public evidence and higher cost, so they belong in later phases.

### Cited Findings
**Benchmarks**
- GameAnalytics data for 2025 across all mobile games: median D1 is about 22% (top 10% about 40%), median D7 about 4% (top 10% about 12%), median D30 about 0.7% (top 10% about 4%). [GameDev Reports / GameAnalytics 2026](https://gamedevreports.substack.com/p/gameanalytics-mobile-and-pc-game)
- A GameAnalytics Q1'24 report found puzzle games had the highest D7 retention of all genres. [GameDev Reports Q1'24](https://gamedevreports.substack.com/p/gameanalytics-benchmarks-in-mobile)
- A third-party (Mistplay-based) puzzle aggregate gives D1 31.85%, D7 12.18% and D30 5.35%. The method is unclear. [Segwise](https://segwise.ai/blog/mobile-gaming-app-user-retention-strategies.md)

**Daily puzzles and streaks**
- NYT Q1 2026: 13.08M total subscribers, 12.52M digital-only, +310k digital net adds. The Times bundles news with games and other content "to broaden appeal and improve retention". [TimesLIVE/Reuters, May 2026](https://www.timeslive.co.za/news/world/2026-05-06-new-york-times-subscribers-cross-13-million-on-news-lifestyle-content-demand/). NYT does not disclose games-only subscriber or retention figures (no primary source found).
- Duolingo's retention team ran 600+ streak experiments over about 4 years. Retention is "most fragile in the first seven days", and the streak was simplified to "completing one lesson a day". [Lenny's Newsletter](https://www.lennysnewsletter.com/p/behind-the-product-duolingo-streaks)
- Unverified secondary claims: streak freeze cut churn 21% for at-risk users, users with a 7-day streak are 3.6x more likely to return daily, and a streak wager added 14% to D14 retention. The design philosophy is "bend, not break". [jtbdux write-up](https://jtbdux-protocol.pages.dev/adoption-and-behavior/behavior-design-patterns/how-duolingo-engineered-50-million-daily-users-and-the-backlash-that-came-with-it). I could not verify these numbers against a primary Duolingo source.
- Competitor minesweeper.now offers a Daily Challenge, daily quests, ranked play and a No-Guessing mode. [minesweeper.now daily](https://minesweeper.now/daily?lang=pt); [about](https://minesweeper.now/about). A 2026 roundup describes minesweeper.online as having leaderboards, stats tracking, daily challenges and achievements. [toolscout roundup](https://toolscout-blog.vercel.app/blog/best-free-minesweeper-games-online) (secondary)

**No-guess boards**
- Simon Tatham's generator embeds a solver that imitates human deduction. When the solver stalls, it moves mines in the unopened area so a new deduction becomes possible. It then re-runs the solver from the original start to confirm the board is solvable without perturbation. Tatham reports it handles mine densities over twice the Windows standard. [Tatham, "Writing a soluble-grid generator for Mines"](https://www.chiark.greenend.org.uk/~sgtatham/quasiblog/mines-solver/); [Puzzles manual](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/doc/mines.html)
- Very dense grids can fall back to "a bare clearing, a tightly packed section round the edges, and a fringe of deductions", which causes visible edge clustering. [2017 commit](https://lists.tartarus.org/pipermail/tartarus-commits/2017-September/013102.html)
- A simpler alternative is regenerate-and-test: minesweeper.now's No-Guessing mode discards the board and generates another whenever its solver would have to guess. [minesweeper.now FAQ](https://minesweeper.now/faq/no-guessing-minesweeper)

**Play Games Services v2**
- PGS v2 attempts automatic sign-in at launch, and apps should show a manual sign-in button as fallback. Saved Games is available, or the PGS player ID can serve as the key for your own cloud save. PGS v2 cannot request extra OAuth scopes. The Recall API links PGS accounts to in-game accounts server-side. A daily login quota applies. [Android Developers: PGS sign-in](https://developer.android.com/games/pgs/signin)

**In-App Review**
- Trigger the review flow after the user has experienced enough of the game. Don't prompt excessively. A time-bound quota applies (a repeat call in under about a month may show nothing). Don't attach the flow to a button. The app should own its timing logic. [Android Developers: In-App Review](https://developer.android.com/guide/playcore/in-app-review)

**Widgets**
- Jetpack Glance builds app widgets with Compose-runtime Kotlin APIs. Some localized doc copies describe it as in active development, and it cannot directly reuse regular Compose UI composables. [Android Developers: Glance](https://developer.android.com/develop/ui/compose/glance?hl=en)

### Inferences
- Daily seeded puzzle with streak, freeze and share (L): this is the highest-leverage feature. It needs the deterministic generator in `feature:game` and a date-to-seed function, so every player gets the same board. A streak model in a new `feature:daily` module or in `feature:home` is persisted to DataStore and Firestore. A streak freeze purchasable with diamonds ties into `feature:wallet`/`feature:store`, and a spoiler-free emoji share grid (Wordle-style) needs no backend. Follow Duolingo's lesson: a "streak" should mean playing one daily board, not winning it.
- No-guess mode (M): start with regenerate-and-test using a simple constraint solver (single-point plus subset rules), keyed off the first click. Tatham's perturbation method can be added later for high-density boards. Lives in `feature:game` (`MinesweeperGame.placeMines`). Make it the default for the daily board so a 50/50 guess never breaks a streak.
- Detailed stats (S-M): win rate per difficulty, best and average times, 3BV/s efficiency, current and longest streak, and a heat map of loss causes. Room already holds history (`GuestGameDao`, `HistoryScreen.kt`). Lives in `feature:game` and `feature:profile`.
- PGS v2 (M): leaderboards per difficulty and for the daily board, mirrored achievements from `AchievementCalculator`, and the player ID as the cloud-save key. Mines already uses Firebase Google auth, so Recall or ID linking avoids two identities. Lives in `feature:auth`, `feature:achievements` and `core:session`.
- In-App Review (S): gate it on, for example, 3+ wins, at least 2 sessions and no recent loss, shown on `feature:celebration` after a win. Keep the existing `feature:userfeedback` screen as the route for unhappy players.
- In-App Updates (S): flexible update on Home, with an immediate update only for breaking economy or backend changes. Lives in `app`/`MainActivity`. (My knowledge, not verified this session: Play In-App Updates supports flexible and immediate flows.)
- Glance widget (M, 3-12 months): "Today's board: unsolved / streak 12 days" plus a tap to play the daily. Low content cost once the daily exists. This would be a new `feature:widget` module.
- Async friend challenges (M): share a seed link. The friend plays the identical board and times are compared. This needs the deterministic seed plus Firebase Dynamic-Links-style deep links (Dynamic Links is deprecated per my knowledge, so use App Links). It becomes the backbone of the "Winter Cup" tournament.
- Real tournaments (L): fixed seeds per round, server-validated times, brackets or leaderboard windows. This fills the `feature:tournament` placeholder. It needs Cloud Functions plus App Check first, otherwise leaderboards will be cheated.

### Gaps
- I found no public, primary retention-lift numbers for daily puzzles (NYT does not disclose them), for Glance widgets, or for PGS integration specifically.
- I did not verify current Glance stability status or widget update-frequency limits.
- The Duolingo numbers are secondary only.

## Q2. Accessibility as features

### Takeaway
Minesweeper communicates almost everything through number colours and tiny cells, so accessibility features translate directly into reach and retention. The core principle across guidelines is double coding: never use colour alone. TalkBack-playable boards and zoom/pan for large boards are differentiators few Minesweeper clones offer.

### Cited Findings
- Xbox Accessibility Guideline 103: colour alone should never be used to represent information, and other channels (sound, shape) should back it up. [Microsoft XAG 103](https://learn.microsoft.com/en-gb/gaming/accessibility/xbox-accessibility-guidelines/103)
- Ian Hamilton's principle is "double-coding": don't use colour difference alone to communicate. A colour-blind mode alone doesn't fix the problem, but redundant cues do. [Colorblind Games 101](https://colorblindgames.com/2021/08/02/colorblind-gaming-101-some-solutions)
- Guidelines recommend testing with colour-blindness simulators during development. [Game Development Wiki: Color Blindness](https://gamedevelopmentwiki.readthedocs.io/en/latest/accessibility/color_blindness.html)
- Compose Multiplatform 1.8 on iOS has stable VoiceOver/accessibility support, relevant if Mines goes cross-platform. [Neowin](https://www.neowin.net/news/jetbrains-brings-compose-for-ios-to-stable-with-compose-multiplatform-180-release/)

### Inferences
- Pre-launch (S): numbers already double-code as digits, so ensure flags and mines also differ by shape, and add a high-contrast palette plus Deuteranopia, Protanopia and Tritanopia palettes as `MinesThemeVariant` entries in `core:theme`. Validate the store skins (`DeepOceanTheme`, `FrostCellSkin`) against the palettes.
- 0-3 months (M): TalkBack board semantics in `feature:game/GameScreen`. Each cell gets a description such as "Row 3, column 5, revealed, 2 mines adjacent" or "flagged", with custom accessibility actions for flag and chord. Add a row/column focus traversal order and haptic feedback distinct for reveal, flag and loss.
- 0-3 months (M): pinch-zoom/pan and a minimum touch-target setting for Expert and large custom boards, a long-press vs. tap swap option, and a flag-mode toggle button (one-handed play). Lives in `feature:game` and `feature:settings`.
- 3-12 months: custom board sizes and a "relaxed" no-timer mode, which also supports cognitive accessibility. Extend `ReduceMotion` to confetti and celebration (`core:ui/ConfettiOverlay`).

### Gaps
- I did not fetch the Game Accessibility Guidelines site itself, and found no data on the share of Minesweeper players who are colour-blind or use TalkBack.

## Q3. A realistic LiveOps blueprint for a 3-person team

### Takeaway
With 3 people, LiveOps must be config-driven and content-light. A daily seeded board, weekly themed challenges and one roughly 4-6 week ocean "season" with a free progression track can run almost entirely from Remote Config plus a seed schedule, with no app releases needed. Tournaments wait until anti-cheat exists.

### Cited Findings
- Remote Config lets teams change app behavior and appearance without publishing an update, with targeting and personalization. [Firebase Remote Config](https://firebase.google.com/products/remote-config)
- Pomelo Games used Remote Config plus A/B Testing to test interstitials across segments, Analytics to measure revenue and retention, and Crashlytics for stability. Firebase reports revenue up to 35% higher with no retention decline (vendor case study). [Firebase blog: Pomelo Games](https://firebase.googleblog.com/2020/09/Pomelo-Games-case-study.html)
- Halfbrick used Remote Config personalization to optimize ad frequency. [Firebase games](https://firebase.blog/posts/2021/04/supercharge-your-mobile-games-business-with-firebase)

### Inferences (proposed blueprint)
- Cadence:
  - Daily: one seeded no-guess board per difficulty tier, generated client-side from a date seed (zero ops cost).
  - Weekly: one "Tide Challenge" modifier, scheduled via a Remote Config JSON calendar. Examples: a fog-of-war board, a time attack, a no-flag run, or a "Kraken" board with a giant mine density.
  - Monthly or seasonal: a season of 4-6 weeks with a new board skin, avatar and cell skin in `feature:store`.
  - Quarterly: the Winter Cup / Spring Cup tournament once server validation exists.
- Ocean meta-progression ideas:
  - "Dive depth" as the level system. The existing `LevelSystem.kt` maps naturally to depth: sunlit zone, twilight zone, midnight zone, abyss and trench, each unlocking a backdrop variant in `AmbientOceanBackdrop`.
  - A collectible "sea creature" album earned from daily streak milestones (7/30/100).
  - A free season "Current Pass" track that pays coins and diamonds, with a cosmetic-only premium track later.
  - "Treasure chests" from cleared boards, tied to wallet.
  - "Shipwreck" weekly puzzles, which are hand-picked interesting seeds.
- Team split: 1 engineer on the client and generator, 1 on backend/Functions/analytics, and 1 for design, art, community and the content calendar. Content is mostly seeds, palettes and skins, so art is the bottleneck. Reuse one art set per season across backdrop, skin and avatar.
- Guardrails: keep a 2-season content buffer, schedule events at least 2 weeks ahead in Remote Config, and keep a kill switch for every event.

### Gaps
- I didn't get to Deconstructor of Fun or GDC sources on small-team LiveOps cadence, so the cadence above is my synthesis, not sourced benchmarks.

## Q4. Firebase tooling: Remote Config, A/B Testing, Crashlytics, Cloud Functions, App Check, FCM

### Takeaway
Add Crashlytics, the Analytics events and Remote Config before launch. These are cheap and unblock everything else. App Check plus Cloud Functions (a server-authoritative wallet, hearts and scores) must come before leaderboards, tournaments or any real-money IAP. Today's client-written Firestore balances make the economy trivially editable. FCM goes in at 0-3 months for streak-at-risk and daily-ready reminders, with tight frequency caps.

### Cited Findings
- App Check with Play Integrity: link the Cloud project in Play Console and register the app's SHA-256. By default App Check requires the Play-recognized app verdict and does not check device integrity, though basic, device or strong integrity can be required. Tokens are sent before enforcement. Monitor request metrics before enforcing, and enforce separately for Cloud Functions. [Firebase App Check Android](https://firebase.google.com/docs/app-check/android)
- App Check proves the request comes from your genuine app, not that the player is honest, so economy changes still need server-side validation. (This is inferred in the search summary from the docs' scope, not a direct quote.) [Firebase App Check Android](https://firebase.google.com/docs/app-check/android)
- Vendor case studies: GameNexa reported 2.5x IAP revenue using Remote Config, A/B Testing and Crashlytics, and Tapps Games used Crashlytics plus Remote Config to raise its crash-free rate and Play rating. [casestudies.com GameNexa](https://www.casestudies.com/company/firebase/case-study/gamenexa-studios-uses-firebase-to-double-in-app-purchase-revenue-and-diversify-monetization-strategy) (self-reported)

### Inferences
- Pre-launch:
  - Crashlytics (S, `app`).
  - Remote Config (S): heart regen interval, `DIAMONDS_PER_HEART`, reward amounts, event calendar and feature flags, read by `feature:life` `LifeRules`, `feature:wallet` and `feature:store`.
  - App Check in monitor mode (S).
- 0-3 months:
  - Cloud Functions (L), as callable functions: `grantReward(gameResultId)`, `spendDiamonds(itemId)`, `consumeHeart()`, `submitDailyScore(seed, moves, time)`.
  - Server-side validation of the daily score by replaying the move log against the seed. This is cheap because the generator is deterministic. Move the generator into a shared pure-Kotlin module to run it on a Kotlin/JVM function, or port it to TS.
  - Lock down Firestore rules so clients cannot write balances.
  - Enforce App Check once metrics are clean.
  - FCM (M): "your streak ends in 3 hours", "new weekly Tide Challenge" and "hearts refilled". Respect Android 13+ POST_NOTIFICATIONS runtime permission (my knowledge) and ask after the first daily win, not at launch.
- 3-12 months: A/B Testing on top of Remote Config and FCM (see Q5).

### Gaps
- I did not fetch current Cloud Functions pricing or Firestore security-rule examples for game currencies, or current FCM best-practice pages.

## Q5. KPI and analytics event plan, plus the first A/B tests

### Takeaway
Instrument a compact event taxonomy now (there are currently zero custom events). Target D1 above 30%, D7 above 10% and D30 above 4%. That is top-quartile-to-top-decile mobile, and plausible for puzzle according to benchmarks. Run the first tests on onboarding difficulty, heart economy and daily-streak forgiveness.

### Cited Findings
- Benchmarks: median D1 22%, D7 4%, D30 0.7%. The top 10% reach 40%, 12% and 4%. [GameAnalytics via GameDev Reports](https://gamedevreports.substack.com/p/gameanalytics-mobile-and-pc-game). Puzzle aggregate: 31.85%, 12.18%, 5.35%. [Segwise](https://segwise.ai/blog/mobile-gaming-app-user-retention-strategies.md)
- Retention is most fragile in the first seven days (Duolingo). [Lenny's Newsletter](https://www.lennysnewsletter.com/p/behind-the-product-duolingo-streaks)
- Firebase A/B Testing works with Remote Config and measures retention and revenue via Analytics. [Pomelo case](https://firebase.googleblog.com/2020/09/Pomelo-Games-case-study.html)

### Inferences
- KPIs:
  - Retention: D1, D7 and D30.
  - Engagement: DAU/MAU stickiness, sessions per DAU, games per session.
  - Win rate per difficulty.
  - Daily board: participation rate and completion rate; streak survival at 7 and 30 days.
  - Hearts: out-of-hearts rate and churn after the out-of-hearts state.
  - Store: conversion, coin sinks vs. sources (economy balance).
  - Platform: crash-free users, ANR rate, Play rating, review-prompt acceptance.
- Event plan (snake_case, minimal params):
  - `tutorial_begin` / `tutorial_complete` (standard Firebase events), `howto_viewed`
  - `game_start` {difficulty, mode: classic|daily|no_guess|challenge|tournament, seed?}
  - `first_click` {ms_to_first_click}
  - `game_end` {result, difficulty, mode, duration_s, 3bv, clicks, flags, hearts_left, cause: mine|timeout|quit}
  - `daily_complete` {streak_len, result}, `streak_broken` {len}, `streak_freeze_used`
  - `hearts_depleted`, `heart_refill` {source: regen|diamonds|ad}
  - `earn_virtual_currency` / `spend_virtual_currency` (standard events) {currency, amount, source}
  - `store_view`, `item_purchase` {item_id, price, currency}
  - `achievement_unlocked` {id, tier}, `level_up` (standard) {level}
  - `share_result`, `challenge_sent` / `challenge_opened`, `review_prompt_shown`, `notification_open` {type}
  - `settings_changed` {key, value} for accessibility adoption (palette, TalkBack-mode, zoom)
- Code placement: put a small `AnalyticsTracker` interface in `core:session` or a new `core:analytics` so feature modules don't depend on Firebase directly.
- First A/B tests, in order:
  1. Heart regen interval of 60 min vs. 30 min vs. 20 min. Metrics: D7 and sessions/DAU, with diamonds spent as a guardrail.
  2. First-session flow: start on Beginner with a no-guess board vs. a difficulty picker. Metric: D1.
  3. Streak forgiveness: one free freeze per week vs. none. Metric: D14/D30 and 7-day streak attainment.
  4. Review-prompt trigger: after the 3rd win vs. after the 1st daily completion. Metrics: rating and prompt rate.
  5. Notification timing: local time of the previous play vs. a fixed 7pm. Metric: daily completion.
- Each test needs a minimum detectable effect sized to install volume. With small indie volumes, prefer few, large-effect tests.

### Gaps
- I found no Minesweeper-specific retention benchmarks. The puzzle-genre aggregate comes from a third party with unclear methodology.

## Q6. Long-term options: iOS and web via Kotlin Multiplatform, cross-platform play

### Takeaway
Compose Multiplatform has been stable for iOS since 1.8.0 (May 2025), so a Compose-heavy, Kotlin-only codebase like Mines is a good KMP candidate. The pragmatic sequence is to extract the pure game engine and economy rules first (they also feed the Cloud Functions validator), then port UI. The web target was not stable as of 1.8.0.

### Cited Findings
- Compose Multiplatform 1.8.0 (May 2025) declared Compose for iOS stable and production-ready, with stable APIs and minimal expected breaking changes. [JetBrains blog](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/)
- JetBrains reports that 96%+ of teams surveyed report no major iOS performance concerns, and that Compose adds about 9 MB versus a native SwiftUI app. [JetBrains blog](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/) (vendor claims)
- iOS accessibility (VoiceOver) is stable. The web target was still Alpha at 1.8.0. It requires Kotlin 2.1.0+ (K2). [Neowin](https://www.neowin.net/news/jetbrains-brings-compose-for-ios-to-stable-with-compose-multiplatform-180-release/); [Seven Peaks](https://sevenpeakssoftware.com/blog/compose-multiplatform-1.8.0-whats-new-and-what-you-need-to-know)
- PGS Recall server integrations are the same across Android and Google Play Games on PC. [Android Developers: PGS on PC](https://developer.android.com/games/playgames/native-pc/pgs)

### Inferences
- Step 1 (3-12 months, M): move `MinesweeperGame`, `ScoreCalculator`, `LevelSystem`, `LifeCalculator` and `AchievementCalculator` into a `:core:engine` KMP module (commonMain, no Android imports). This unblocks server validation, iOS and web, and makes unit testing easier.
- Step 2 (long-term, L): iOS app with Compose Multiplatform. Replace Room/DataStore with KMP-capable equivalents (Room and DataStore have KMP support per my knowledge, not verified here). Firebase on iOS needs a KMP wrapper or expect/actual. Swap Google sign-in for Sign in with Apple, plus Game Center in place of PGS.
- Step 3 (long-term): a web daily-puzzle page (Wasm/JS) for shareable challenge links and SEO. Web is the main acquisition channel for daily puzzles (Wordle/NYT pattern). Check the current maturity of the Compose web target before committing.
- Google Play Games on PC is a low-effort extra surface once PGS v2 is integrated.
- Cross-platform play only matters for async challenges and leaderboards, which work cross-platform automatically if the backend is Firestore plus Functions and identity is Firebase Auth.

### Gaps
- I did not verify the Compose Multiplatform release status after 1.8 (web Beta or stable as of Nov 2026), or Firebase KMP SDK maturity.

## Q7. Phased, prioritized feature backlog (the objective)

### Takeaway
Pre-launch focuses on foundations: a deterministic generator, analytics, Crashlytics, Remote Config, colour-safe palettes and a review prompt. Months 0-3 add the daily + streak + no-guess core loop with server-authoritative economy, PGS v2 and notifications. Months 3-12 bring seasons, async challenges, real tournaments, widgets and the KMP engine. Long-term covers iOS, web and cross-platform. Rationale and evidence are in Q1-Q6.

### Cited Findings
- See per-item evidence in Q1-Q6. Key anchors:
  - Daily/streak lessons. [Lenny's](https://www.lennysnewsletter.com/p/behind-the-product-duolingo-streaks)
  - No-guess generation. [Tatham](https://www.chiark.greenend.org.uk/~sgtatham/quasiblog/mines-solver/)
  - Competitors already ship daily and no-guess modes. [minesweeper.now](https://minesweeper.now/about)
  - Review API rules. [Android](https://developer.android.com/guide/playcore/in-app-review)
  - PGS v2. [Android](https://developer.android.com/games/pgs/signin)
  - App Check. [Firebase](https://firebase.google.com/docs/app-check/android)
  - Compose Multiplatform for iOS stable. [JetBrains](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/)

### Inferences (backlog: priority / feature / effort / module / rationale)

**Pre-launch**
| # | Feature | Effort | Module(s) | Rationale |
|---|---|---|---|---|
| P1 | Seedable deterministic generator (seed + first-click -> board) | S | feature:game | Prerequisite for daily, challenges, tournaments and server validation |
| P2 | Analytics event taxonomy plus `AnalyticsTracker` abstraction | S | core:session or new core:analytics, all features | Currently zero events; no decision is possible without them |
| P3 | Crashlytics | S | app | Stability and Play vitals |
| P4 | Remote Config for economy constants and feature flags | S | feature:life, wallet, store | Tune the 1h regen and prices without a release |
| P5 | Colour-blind and high-contrast palettes; shape-coded flags and mines | S | core:theme, feature:store skins | Double-coding guideline |
| P6 | In-App Review gated after wins | S | feature:celebration | Official guidance; ratings drive conversion |
| P7 | In-App Updates (flexible) | S | app | Needed before server-side economy changes |
| P8 | App Check (monitor mode) | S | app | Gather metrics before enforcing |
| P9 | Tournament screen: keep "Coming soon" but add "Notify me" (FCM topic or opt-in) | S | feature:tournament | Turns the placeholder into a demand signal |

**0-3 months**
| # | Feature | Effort | Module(s) | Rationale |
|---|---|---|---|---|
| A1 | Daily seeded no-guess board, streak, 1 free freeze/week, emoji share | L | feature:game, home, new feature:daily, wallet | Strongest retention evidence (NYT/Duolingo); competitors have it |
| A2 | No-guess mode (regenerate-and-test solver) | M | feature:game | Fairness; genre-expected; required for streaks |
| A3 | Detailed stats (win rate, best and average times, 3BV/s, streaks) | M | feature:game (Room), profile | Mastery motivation; data already in Room |
| A4 | Cloud Functions server-authoritative wallet, hearts and rewards; locked Firestore rules; enforce App Check | L | wallet, life, store, backend | Client-written balances are exploitable; prerequisite for leaderboards and IAP |
| A5 | PGS v2: auto sign-in, leaderboards (daily + per difficulty), achievements mirror, cloud save keyed by player ID | M | auth, achievements, core:session | Platform discovery and identity |
| A6 | FCM: streak-at-risk, daily-ready, hearts-full (capped, opt-in after first daily) | M | new feature:notifications, life | D7 retention, which is fragile in week 1 |
| A7 | TalkBack board semantics, zoom/pan, touch-target size, flag-mode toggle | M | feature:game, settings | Accessibility as differentiator |
| A8 | First A/B tests (heart regen, onboarding, streak freeze) | S each | Remote Config | Data-driven tuning |

**3-12 months**
| # | Feature | Effort | Module(s) | Rationale |
|---|---|---|---|---|
| B1 | Ocean seasons: "Dive depth" levels, creature album, free Current Pass, seasonal skins | L | feature:store, achievements, core:ui backdrops, LevelSystem | LiveOps cadence for a 3-person team |
| B2 | Weekly "Tide Challenge" modifiers via Remote Config calendar | M | feature:game, home | Config-driven content; low ops |
| B3 | Async friend challenges via seed deep links (App Links) | M | feature:game, new feature:social | Viral loop; reuses the seed |
| B4 | Real tournaments (Winter Cup): fixed seeds, server-validated replay, time-boxed leaderboards, rewards | L | feature:tournament, backend | Fills the placeholder; needs A4 |
| B5 | Glance widget (today's daily + streak) | M | new feature:widget | Home-screen re-entry point |
| B6 | Extract `:core:engine` KMP module (game, score, level, life rules) | M | feature:game, life, achievements | Shared with server validator; prepares iOS and web |
| B7 | Custom boards, relaxed no-timer mode, puzzle archive (past dailies) | M | feature:game | Depth for core fans; archive as soft monetization |
| B8 | Tatham-style perturbation generator for high-density no-guess | M | core:engine | Expert-level fairness |

**Long-term (12+ months)**
| # | Feature | Effort | Module(s) | Rationale |
|---|---|---|---|---|
| C1 | iOS via Compose Multiplatform (Game Center, Sign in with Apple) | L | all | CMP iOS stable since May 2025 |
| C2 | Web daily-puzzle page (Wasm) for shareable links and SEO | L | core:engine plus web | Daily puzzles spread on the web |
| C3 | Cross-platform leaderboards and challenges on a shared Firebase backend | M | backend | Comes nearly free once A4/B3/B4 exist |
| C4 | Google Play Games on PC | M | app | Same PGS Recall integration |
| C5 | Clubs or friend leagues (weekly league tables à la Duolingo) | L | social, backend | Social retention, once the DAU base exists |

### Gaps
- Effort sizes are my estimates for a 3-person team, not sourced.
- No sourced uplift numbers exist for widgets, PGS, or tournaments in casual puzzle games.
- Monetization specifics (IAP and ads) are covered by the sibling note `market_popularity_monetization.md` and are not repeated here.
